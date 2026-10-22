package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;

import java.util.Locale;

public enum TipoDocumento {
    PDF,
    MARKDOWN,
    TEXTO;

    public static TipoDocumento pelaExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            throw new Excecoes.RegraDeNegocio("O arquivo precisa ter extensão .pdf, .md ou .txt.");
        }
        String extensao = nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extensao) {
            case "pdf" -> PDF;
            case "md", "markdown" -> MARKDOWN;
            case "txt" -> TEXTO;
            default -> throw new Excecoes.RegraDeNegocio("Formato ." + extensao + " não suportado. Use .pdf, .md ou .txt.");
        };
    }
}
