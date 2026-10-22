package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtratorDeTextoTest {

    private final ExtratorDeTexto extrator = new ExtratorDeTexto();

    @Test
    void extraiTextoDeMarkdown() {
        byte[] bytes = "# Título\n\nConteúdo com acentuação.".getBytes(StandardCharsets.UTF_8);
        assertThat(extrator.extrair(TipoDocumento.MARKDOWN, bytes)).contains("Conteúdo com acentuação.");
    }

    @Test
    void extraiTextoDePdf() throws Exception {
        assertThat(extrator.extrair(TipoDocumento.PDF, pdfCom("Politica de ferias da empresa")))
                .contains("Politica de ferias da empresa");
    }

    @Test
    void pdfCorrompidoGeraErroClaro() {
        assertThatThrownBy(() -> extrator.extrair(TipoDocumento.PDF, "isto não é um pdf".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Não foi possível ler o PDF");
    }

    @Test
    void tipoEhIdentificadoPelaExtensao() {
        assertThat(TipoDocumento.pelaExtensao("Manual.PDF")).isEqualTo(TipoDocumento.PDF);
        assertThat(TipoDocumento.pelaExtensao("notas.md")).isEqualTo(TipoDocumento.MARKDOWN);
        assertThat(TipoDocumento.pelaExtensao("leia.txt")).isEqualTo(TipoDocumento.TEXTO);
    }

    @Test
    void extensaoNaoSuportadaEhRecusada() {
        assertThatThrownBy(() -> TipoDocumento.pelaExtensao("planilha.xlsx")).isInstanceOf(Excecoes.RegraDeNegocio.class);
        assertThatThrownBy(() -> TipoDocumento.pelaExtensao("sem-extensao")).isInstanceOf(Excecoes.RegraDeNegocio.class);
    }

    private static byte[] pdfCom(String texto) throws Exception {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                conteudo.newLineAtOffset(50, 700);
                conteudo.showText(texto);
                conteudo.endText();
            }
            documento.save(saida);
            return saida.toByteArray();
        }
    }
}
