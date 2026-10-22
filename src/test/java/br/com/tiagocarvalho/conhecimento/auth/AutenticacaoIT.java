package br.com.tiagocarvalho.conhecimento.auth;

import br.com.tiagocarvalho.conhecimento.suporte.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacaoIT extends IntegracaoBase {

    @Test
    void registraEntraEConsultaOsPropriosDados() throws Exception {
        String email = "maria-" + UUID.randomUUID() + "@teste.com";
        mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"%s\",\"senha\":\"senha-forte-123\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("USUARIO"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        String token = entrar(email, "senha-forte-123");

        mvc.perform(get("/api/auth/eu").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("USUARIO"));
    }

    @Test
    void emailRepetidoRetorna409() throws Exception {
        String corpo = "{\"nome\":\"João\",\"email\":\"joao-%s@teste.com\",\"senha\":\"senha-forte-123\"}"
                .formatted(UUID.randomUUID());
        mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(post("/api/auth/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"senha-errada\"}".formatted(EMAIL_ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/api/documentos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mvc.perform(get("/api/documentos").header(HttpHeaders.AUTHORIZATION, "Bearer token.que.nao.vale"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComumPodeListarMasNaoEnviarDocumentos() throws Exception {
        String token = tokenUsuarioNovo();

        mvc.perform(get("/api/documentos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        enviarArquivo(token, "notas.md", "Notas", "conteúdo qualquer " + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }
}
