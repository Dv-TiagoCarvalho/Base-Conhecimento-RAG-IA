package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.documento.StatusDocumento;
import br.com.tiagocarvalho.conhecimento.suporte.IntegracaoBase;
import br.com.tiagocarvalho.conhecimento.suporte.ModeloFalso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerguntasIT extends IntegracaoBase {

    private static final String PERGUNTA = "Quantos dias de férias o colaborador tem?";

    private String token;
    private UUID documentoId;

    /** Cada teste envia o próprio manual (com um código único, porque arquivos repetidos são recusados). */
    @BeforeEach
    void prepararDocumento() throws Exception {
        token = tokenAdmin();
        documentoId = enviarDocumento(token, "ferias.md", "Manual de RH", """
                # Manual de RH %s

                Todo colaborador tem direito a trinta dias de férias remuneradas por ano.

                O vale-refeição é creditado no quinto dia útil de cada mês.
                """.formatted(UUID.randomUUID()));
        aguardarStatus(documentoId, StatusDocumento.PRONTO);
    }

    /** Remove o manual no fim, senão as cópias dos testes anteriores competiriam na busca. */
    @AfterEach
    void removerDocumento() {
        documentos.deleteById(documentoId);
    }

    @Test
    void respondeComBaseNosTrechosECitaAFonte() throws Exception {
        perguntar(token, PERGUNTA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("COMPLETA"))
                .andExpect(jsonPath("$.resposta").value(ModeloFalso.RESPOSTA_PADRAO))
                .andExpect(jsonPath("$.fontes[*].documentoId", hasItem(documentoId.toString())))
                .andExpect(jsonPath("$.fontes[0].citada").value(true))
                .andExpect(jsonPath("$.doCache").value(false));

        // o modelo recebeu o trecho certo no prompt
        assertThat(modeloFalso.ultimaMensagem()).contains("trinta dias de férias remuneradas");
    }

    @Test
    void perguntaRepetidaVemDoCacheSemChamarOModelo() throws Exception {
        perguntar(token, PERGUNTA).andExpect(jsonPath("$.doCache").value(false));
        perguntar(token, "  quantos dias de FÉRIAS o colaborador tem  ").andExpect(jsonPath("$.doCache").value(true));

        assertThat(modeloFalso.chamadas()).isEqualTo(1);
    }

    @Test
    void perguntaSemRelacaoNaoChamaOModelo() throws Exception {
        perguntar(token, "Quantos planetas orbitam Júpiter")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("SEM_CONTEXTO"))
                .andExpect(jsonPath("$.fontes", empty()));

        assertThat(modeloFalso.chamadas()).isZero();
    }

    @Test
    void quandoOModeloFalhaDevolveSoOsTrechos() throws Exception {
        modeloFalso.simularFalha();

        perguntar(token, PERGUNTA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("SOMENTE_TRECHOS"))
                .andExpect(jsonPath("$.fontes", not(empty())));
    }

    @Test
    void respostaSemFonteEhRejeitada() throws Exception {
        modeloFalso.responderCom("São trinta dias.");

        perguntar(token, PERGUNTA)
                .andExpect(jsonPath("$.modo").value("SOMENTE_TRECHOS"));
    }

    @Test
    void respostaQueCitaTrechoInexistenteEhRejeitada() throws Exception {
        modeloFalso.responderCom("São trinta dias [99].");

        perguntar(token, PERGUNTA)
                .andExpect(jsonPath("$.modo").value("SOMENTE_TRECHOS"));
    }

    @Test
    void respostaRejeitadaNaoVaiParaOCache() throws Exception {
        modeloFalso.simularFalha();
        perguntar(token, PERGUNTA).andExpect(jsonPath("$.modo").value("SOMENTE_TRECHOS"));

        modeloFalso.reiniciar();
        perguntar(token, PERGUNTA)
                .andExpect(jsonPath("$.modo").value("COMPLETA"))
                .andExpect(jsonPath("$.doCache").value(false));
    }

    @Test
    void perguntaVaziaRetorna400() throws Exception {
        perguntar(token, "   ").andExpect(status().isBadRequest());
    }

    @Test
    void usuarioComumTambemPodePerguntar() throws Exception {
        perguntar(tokenUsuarioNovo(), PERGUNTA).andExpect(status().isOk());
    }
}
