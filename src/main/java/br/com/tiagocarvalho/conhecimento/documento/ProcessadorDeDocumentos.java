package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.config.RabbitConfig;
import br.com.tiagocarvalho.conhecimento.ia.GeradorDeEmbeddings;
import br.com.tiagocarvalho.conhecimento.pergunta.CacheDeRespostas;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Consome a fila "documentos.processar": extrai o texto, divide em trechos, gera os embeddings
 * e grava tudo no banco.
 *
 * Decisões importantes:
 * - Idempotente: se a mesma mensagem chegar duas vezes, o resultado é o mesmo (documento PRONTO é ignorado
 *   e os trechos antigos são apagados antes de gravar os novos).
 * - As chamadas lentas (Ollama) acontecem FORA da transação do banco, para não segurar conexão aberta.
 * - Em caso de erro, o documento fica FALHOU com o motivo, e a mensagem vai para a fila de mensagens mortas.
 */
@Component
public class ProcessadorDeDocumentos {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorDeDocumentos.class);

    private final RepositorioDocumentos documentos;
    private final RepositorioTrechos trechos;
    private final ExtratorDeTexto extrator;
    private final DivisorDeTexto divisor;
    private final GeradorDeEmbeddings embeddings;
    private final CacheDeRespostas cache;
    private final TransactionTemplate transacao;
    private final MeterRegistry metricas;

    public ProcessadorDeDocumentos(RepositorioDocumentos documentos, RepositorioTrechos trechos,
                                   ExtratorDeTexto extrator, DivisorDeTexto divisor, GeradorDeEmbeddings embeddings,
                                   CacheDeRespostas cache, TransactionTemplate transacao, MeterRegistry metricas) {
        this.documentos = documentos;
        this.trechos = trechos;
        this.extrator = extrator;
        this.divisor = divisor;
        this.embeddings = embeddings;
        this.cache = cache;
        this.transacao = transacao;
        this.metricas = metricas;
    }

    @RabbitListener(queues = RabbitConfig.FILA)
    public void processar(Mensagens.ProcessarDocumento mensagem) {
        UUID id = mensagem.documentoId();
        Documento documento = documentos.findById(id).orElse(null);
        if (documento == null) {
            log.warn("Documento {} não existe mais; mensagem descartada.", id);
            return;
        }
        if (documento.getStatus() == StatusDocumento.PRONTO) {
            log.info("Documento {} já está pronto; mensagem repetida ignorada.", id);
            return;
        }

        alterarDocumento(id, Documento::marcarProcessando);
        long inicio = System.nanoTime();
        try {
            String texto = extrator.extrair(documento.getTipo(), documento.getConteudo());
            List<String> partes = divisor.dividir(texto);
            if (partes.isEmpty()) {
                throw new IllegalStateException("Não foi encontrado texto no arquivo (PDF escaneado como imagem?).");
            }
            List<float[]> vetores = embeddings.gerarVarios(partes);

            transacao.executeWithoutResult(status -> {
                trechos.removerDoDocumento(id);
                trechos.inserir(id, partes, vetores);
                documentos.findById(id).ifPresent(d -> d.marcarPronto(partes.size()));
            });
            cache.limpar();   // um documento novo pode mudar respostas que estavam em cache

            metricas.timer("documentos.processamento.tempo").record(java.time.Duration.ofNanos(System.nanoTime() - inicio));
            metricas.counter("documentos.processados", "resultado", "sucesso").increment();
            log.info("Documento {} processado: {} trechos.", id, partes.size());
        } catch (RuntimeException e) {
            log.error("Falha ao processar o documento {}", id, e);
            alterarDocumento(id, d -> d.marcarFalha(e.getMessage()));
            metricas.counter("documentos.processados", "resultado", "falha").increment();
            // Rejeita sem devolver para a fila: a mensagem vai para a fila de mensagens mortas (DLQ)
            throw new AmqpRejectAndDontRequeueException("Falha ao processar o documento " + id, e);
        }
    }

    private void alterarDocumento(UUID id, java.util.function.Consumer<Documento> alteracao) {
        transacao.executeWithoutResult(status -> documentos.findById(id).ifPresent(alteracao));
    }
}
