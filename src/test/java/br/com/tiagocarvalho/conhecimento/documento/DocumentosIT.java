package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.suporte.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentosIT extends IntegracaoBase {

    @Autowired
    private RepositorioTrechos trechos;

    @Autowired
    private ProcessadorDeDocumentos processador;

    @Test
    void documentoEnviadoEhProcessadoPelaFila() throws Exception {
        String token = tokenAdmin();
        UUID id = enviarDocumento(token, "rh.md", "Manual " + UUID.randomUUID(), """
                # Manual

                Texto do primeiro parágrafo sobre benefícios. %s

                Texto do segundo parágrafo sobre horários.
                """.formatted(UUID.randomUUID()));

        Documento documento = aguardarStatus(id, StatusDocumento.PRONTO);

        assertThat(documento.getQuantidadeTrechos()).isPositive();
        assertThat(trechos.contarDoDocumento(id)).isEqualTo(documento.getQuantidadeTrechos());

        mvc.perform(get("/api/documentos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id.toString())));
    }

    @Test
    void mensagemRepetidaNaoDuplicaTrechos() throws Exception {
        UUID id = enviarDocumento(tokenAdmin(), "repetido.txt", "Repetido", "Conteúdo para idempotência " + UUID.randomUUID());
        aguardarStatus(id, StatusDocumento.PRONTO);
        int antes = trechos.contarDoDocumento(id);

        processador.processar(new Mensagens.ProcessarDocumento(id));   // mesma mensagem de novo

        assertThat(trechos.contarDoDocumento(id)).isEqualTo(antes);
    }

    @Test
    void arquivoSemTextoTerminaComFalha() throws Exception {
        // só espaços: tem bytes, mas nenhum texto aproveitável (o UUID garante que o hash é único)
        String vazio = " ".repeat(5 + Math.floorMod(UUID.randomUUID().hashCode(), 5000)) + "\n\n";
        UUID id = enviarDocumento(tokenAdmin(), "vazio.txt", "Vazio", vazio);

        Documento documento = aguardarStatus(id, StatusDocumento.FALHOU);

        assertThat(documento.getMensagemErro()).contains("Não foi encontrado texto");
    }

    @Test
    void mesmoArquivoDuasVezesRetorna409() throws Exception {
        String token = tokenAdmin();
        String conteudo = "Arquivo único " + UUID.randomUUID();
        enviarDocumento(token, "a.txt", "A", conteudo);

        enviarArquivo(token, "b.txt", "B", conteudo).andExpect(status().isConflict());
    }

    @Test
    void formatoNaoSuportadoRetorna400() throws Exception {
        enviarArquivo(tokenAdmin(), "planilha.xlsx", "Planilha", "qualquer coisa " + UUID.randomUUID())
                .andExpect(status().isBadRequest());
    }

    @Test
    void documentoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/documentos/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void documentoProntoNaoPodeSerReprocessado() throws Exception {
        String token = tokenAdmin();
        UUID id = enviarDocumento(token, "pronto.txt", "Pronto", "Texto pronto " + UUID.randomUUID());
        aguardarStatus(id, StatusDocumento.PRONTO);

        mvc.perform(post("/api/documentos/" + id + "/reprocessar").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removerApagaOsTrechos() throws Exception {
        String token = tokenAdmin();
        UUID id = enviarDocumento(token, "remover.txt", "Remover", "Texto que será apagado " + UUID.randomUUID());
        aguardarStatus(id, StatusDocumento.PRONTO);

        mvc.perform(delete("/api/documentos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(documentos.findById(id)).isEmpty();
        assertThat(trechos.contarDoDocumento(id)).isZero();
    }
}
