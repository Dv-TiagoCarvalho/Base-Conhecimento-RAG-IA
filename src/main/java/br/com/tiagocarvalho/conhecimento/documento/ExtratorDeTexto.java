package br.com.tiagocarvalho.conhecimento.documento;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Tira o texto puro de cada tipo de arquivo aceito. */
@Component
public class ExtratorDeTexto {

    public String extrair(TipoDocumento tipo, byte[] conteudo) {
        return switch (tipo) {
            case PDF -> extrairPdf(conteudo);
            case MARKDOWN, TEXTO -> new String(conteudo, StandardCharsets.UTF_8);
        };
    }

    private String extrairPdf(byte[] conteudo) {
        try (PDDocument pdf = Loader.loadPDF(conteudo)) {
            return new PDFTextStripper().getText(pdf);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o PDF: " + e.getMessage(), e);
        }
    }
}
