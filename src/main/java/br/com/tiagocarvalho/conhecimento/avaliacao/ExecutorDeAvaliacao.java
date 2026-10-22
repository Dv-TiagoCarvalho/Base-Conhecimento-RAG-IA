package br.com.tiagocarvalho.conhecimento.avaliacao;

import br.com.tiagocarvalho.conhecimento.pergunta.CacheDeRespostas;
import br.com.tiagocarvalho.conhecimento.pergunta.RespostaPergunta;
import br.com.tiagocarvalho.conhecimento.pergunta.ServicoPerguntas;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Roda um conjunto fixo de perguntas com respostas esperadas e mede a qualidade do sistema.
 * Serve para comparar mudanças (outro tamanho de trecho, outro limite de similaridade, outro modelo)
 * com números, em vez de "achismo".
 *
 * Só é ativado com o perfil "avaliacao". Veja avaliacao/README.md.
 */
@Component
@Profile("avaliacao")
public class ExecutorDeAvaliacao implements ApplicationRunner {

    /** documentoEsperado = null significa que a resposta certa é "não sei" (SEM_CONTEXTO). */
    public record Caso(String pergunta, String documentoEsperado, List<String> palavrasEsperadas) {
    }

    private final ServicoPerguntas servico;
    private final CacheDeRespostas cache;
    private final ObjectMapper json;
    private final ConfigurableApplicationContext contexto;
    private final String arquivo;

    public ExecutorDeAvaliacao(ServicoPerguntas servico, CacheDeRespostas cache, ObjectMapper json,
                               ConfigurableApplicationContext contexto,
                               @Value("${app.avaliacao.arquivo}") String arquivo) {
        this.servico = servico;
        this.cache = cache;
        this.json = json;
        this.contexto = contexto;
        this.arquivo = arquivo;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Caso> casos = json.readValue(new File(arquivo), new TypeReference<List<Caso>>() {
        });
        cache.limpar();

        int acertosBusca = 0;
        int acertosResposta = 0;
        System.out.println();
        System.out.println("=== Avaliação: " + casos.size() + " perguntas ===");

        for (Caso caso : casos) {
            RespostaPergunta r = servico.responder(caso.pergunta());
            boolean buscaOk;
            boolean respostaOk;

            if (caso.documentoEsperado() == null) {
                buscaOk = r.modo() == RespostaPergunta.Modo.SEM_CONTEXTO;
                respostaOk = buscaOk;
            } else {
                buscaOk = r.fontes().stream().anyMatch(f -> f.documento().equalsIgnoreCase(caso.documentoEsperado()));
                String texto = r.resposta().toLowerCase(Locale.ROOT);
                List<String> palavras = caso.palavrasEsperadas() == null ? List.of() : caso.palavrasEsperadas();
                respostaOk = r.modo() == RespostaPergunta.Modo.COMPLETA
                        && palavras.stream().allMatch(p -> texto.contains(p.toLowerCase(Locale.ROOT)));
            }
            if (buscaOk) {
                acertosBusca++;
            }
            if (respostaOk) {
                acertosResposta++;
            }
            System.out.printf("[%s|%s] %-16s %s%n", buscaOk ? "busca OK " : "busca ERR",
                    respostaOk ? "resposta OK " : "resposta ERR", r.modo(), caso.pergunta());
        }

        System.out.println("-----------------------------------------------");
        System.out.printf("Busca encontrou o documento certo: %d/%d (%.0f%%)%n",
                acertosBusca, casos.size(), 100.0 * acertosBusca / casos.size());
        System.out.printf("Resposta correta:                  %d/%d (%.0f%%)%n",
                acertosResposta, casos.size(), 100.0 * acertosResposta / casos.size());
        System.out.println();

        System.exit(SpringApplication.exit(contexto, () -> 0));
    }
}
