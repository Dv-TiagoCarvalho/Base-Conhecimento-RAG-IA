package br.com.tiagocarvalho.conhecimento.documento;

import java.time.Instant;
import java.util.UUID;

/** O que a API devolve sobre um documento. Nunca inclui o conteúdo do arquivo. */
public record DocumentoResposta(
        UUID id,
        String titulo,
        String nomeArquivo,
        TipoDocumento tipo,
        StatusDocumento status,
        Integer quantidadeTrechos,
        String mensagemErro,
        Instant criadoEm,
        Instant processadoEm) {

    public static DocumentoResposta de(Documento d) {
        return new DocumentoResposta(d.getId(), d.getTitulo(), d.getNomeArquivo(), d.getTipo(), d.getStatus(),
                d.getQuantidadeTrechos(), d.getMensagemErro(), d.getCriadoEm(), d.getProcessadoEm());
    }
}
