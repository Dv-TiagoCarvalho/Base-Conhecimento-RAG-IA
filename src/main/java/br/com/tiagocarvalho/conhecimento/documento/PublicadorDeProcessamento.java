package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica a mensagem no RabbitMQ somente DEPOIS que a transação do banco foi confirmada (AFTER_COMMIT).
 *
 * Se publicasse antes, o processador poderia receber a mensagem e buscar um documento
 * que ainda não existe no banco, ou que nunca vai existir porque a transação falhou.
 */
@Component
public class PublicadorDeProcessamento {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDeProcessamento.class);

    private final RabbitTemplate rabbit;

    public PublicadorDeProcessamento(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoSalvarDocumento(Mensagens.DocumentoAguardandoProcessamento evento) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROTA,
                    new Mensagens.ProcessarDocumento(evento.documentoId()));
        } catch (RuntimeException e) {
            // O documento continua PENDENTE no banco e pode ser reenviado com POST /api/documentos/{id}/reprocessar
            log.error("Não foi possível publicar o documento {} na fila: {}", evento.documentoId(), e.getMessage());
        }
    }
}
