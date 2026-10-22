package br.com.tiagocarvalho.conhecimento.documento;

import java.util.UUID;

/** Eventos e mensagens trocados durante o processamento de documentos. */
public final class Mensagens {

    private Mensagens() {
    }

    /** Evento interno do Spring: "um documento entrou na fila do banco". */
    public record DocumentoAguardandoProcessamento(UUID documentoId) {
    }

    /** Mensagem publicada no RabbitMQ para o processador. */
    public record ProcessarDocumento(UUID documentoId) {
    }
}
