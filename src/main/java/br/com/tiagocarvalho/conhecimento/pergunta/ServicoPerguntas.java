package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.auth.UsuarioAutenticado;
import br.com.tiagocarvalho.conhecimento.config.Propriedades;
import br.com.tiagocarvalho.conhecimento.documento.RepositorioTrechos;
import br.com.tiagocarvalho.conhecimento.documento.TrechoEncontrado;
import br.com.tiagocarvalho.conhecimento.ia.GeradorDeEmbeddings;
import br.com.tiagocarvalho.conhecimento.ia.ModeloDeLinguagem;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * O fluxo do RAG:
 * 1. limite de requisições e cache;
 * 2. pergunta -> embedding (Ollama);
 * 3. busca dos trechos mais parecidos (pgvector);
 * 4. sem trecho relevante -> responde "sem contexto" sem chamar o Claude (economiza e evita invenção);
 * 5. com trechos -> chama o Claude, valida a resposta e, se algo der errado, cai para o plano B
 *    (devolver só os trechos encontrados).
 */
@Service
public class ServicoPerguntas {

    private static final Logger log = LoggerFactory.getLogger(ServicoPerguntas.class);

    static final String MSG_SEM_CONTEXTO =
            "Não encontrei nos documentos cadastrados nenhum trecho relacionado a essa pergunta.";
    static final String MSG_MODELO_INDISPONIVEL =
            "O modelo de IA não conseguiu responder agora. Veja abaixo os trechos mais relevantes encontrados.";
    static final String MSG_RESPOSTA_REJEITADA =
            "A resposta gerada não passou na validação. Veja abaixo os trechos mais relevantes encontrados.";

    private final GeradorDeEmbeddings embeddings;
    private final ModeloDeLinguagem modelo;
    private final RepositorioTrechos trechos;
    private final CacheDeRespostas cache;
    private final LimitadorDeRequisicoes limitador;
    private final Propriedades.Rag rag;
    private final MeterRegistry metricas;

    public ServicoPerguntas(GeradorDeEmbeddings embeddings, ModeloDeLinguagem modelo, RepositorioTrechos trechos,
                            CacheDeRespostas cache, LimitadorDeRequisicoes limitador, Propriedades.Rag rag,
                            MeterRegistry metricas) {
        this.embeddings = embeddings;
        this.modelo = modelo;
        this.trechos = trechos;
        this.cache = cache;
        this.limitador = limitador;
        this.rag = rag;
        this.metricas = metricas;
    }

    public RespostaPergunta perguntar(String pergunta, UsuarioAutenticado usuario) {
        limitador.verificar(usuario.id().toString());
        return responder(pergunta);
    }

    /** Sem limite de requisições: usado também pelo executor de avaliação. */
    public RespostaPergunta responder(String pergunta) {
        String chave = CacheDeRespostas.normalizar(pergunta);
        Optional<RespostaPergunta> emCache = cache.obter(chave);
        if (emCache.isPresent()) {
            metricas.counter("rag.cache.acertos").increment();
            return emCache.get().vindaDoCache();
        }

        float[] vetor = medir("rag.embedding.tempo", () -> embeddings.gerar(pergunta));
        List<TrechoEncontrado> encontrados = medir("rag.busca.tempo",
                () -> trechos.buscarSemelhantes(vetor, rag.topK(), rag.similaridadeMinima()));

        RespostaPergunta resposta = encontrados.isEmpty()
                ? new RespostaPergunta(MSG_SEM_CONTEXTO, RespostaPergunta.Modo.SEM_CONTEXTO, List.of(), false)
                : gerarComModelo(pergunta, encontrados);

        metricas.counter("rag.respostas", "modo", resposta.modo().name()).increment();
        if (resposta.modo() == RespostaPergunta.Modo.COMPLETA) {
            cache.guardar(chave, resposta);
        }
        return resposta;
    }

    private RespostaPergunta gerarComModelo(String pergunta, List<TrechoEncontrado> encontrados) {
        String mensagem = MontadorDePrompt.montarMensagem(pergunta, encontrados);
        String texto;
        try {
            texto = medir("rag.llm.tempo", () -> modelo.responder(MontadorDePrompt.INSTRUCOES, mensagem));
        } catch (RuntimeException e) {
            log.warn("Falha ao chamar o modelo de linguagem: {}", e.getMessage());
            return somenteTrechos(encontrados, MSG_MODELO_INDISPONIVEL);
        }

        ValidadorDeResposta.Resultado validacao = ValidadorDeResposta.validar(texto, encontrados.size());
        if (!validacao.valida()) {
            log.warn("Resposta do modelo rejeitada: {}", validacao.motivo());
            return somenteTrechos(encontrados, MSG_RESPOSTA_REJEITADA);
        }
        RespostaPergunta.Modo modo = validacao.naoEncontrado()
                ? RespostaPergunta.Modo.SEM_CONTEXTO
                : RespostaPergunta.Modo.COMPLETA;
        return new RespostaPergunta(texto.trim(), modo, fontes(encontrados, validacao.citados()), false);
    }

    private RespostaPergunta somenteTrechos(List<TrechoEncontrado> encontrados, String aviso) {
        return new RespostaPergunta(aviso, RespostaPergunta.Modo.SOMENTE_TRECHOS, fontes(encontrados, Set.of()), false);
    }

    private static List<RespostaPergunta.Fonte> fontes(List<TrechoEncontrado> encontrados, Set<Integer> citados) {
        List<RespostaPergunta.Fonte> fontes = new ArrayList<>();
        for (int i = 0; i < encontrados.size(); i++) {
            TrechoEncontrado t = encontrados.get(i);
            int numero = i + 1;
            fontes.add(new RespostaPergunta.Fonte(numero, t.documentoId(), t.documento(), t.indice() + 1,
                    t.conteudo(), arredondar(t.similaridade()), citados.contains(numero)));
        }
        return fontes;
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }

    private <T> T medir(String nomeMetrica, Supplier<T> acao) {
        long inicio = System.nanoTime();
        try {
            return acao.get();
        } finally {
            metricas.timer(nomeMetrica).record(Duration.ofNanos(System.nanoTime() - inicio));
        }
    }
}
