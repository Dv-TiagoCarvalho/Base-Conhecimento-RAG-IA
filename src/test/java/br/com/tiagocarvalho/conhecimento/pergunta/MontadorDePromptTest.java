package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.documento.TrechoEncontrado;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MontadorDePromptTest {

    @Test
    void numeraOsTrechosEIncluiAPergunta() {
        var trechos = List.of(
                new TrechoEncontrado(UUID.randomUUID(), "Manual de RH", 0, "Férias de 30 dias.", 0.9),
                new TrechoEncontrado(UUID.randomUUID(), "Política de TI", 4, "Senha com 12 caracteres.", 0.7));

        String mensagem = MontadorDePrompt.montarMensagem("  Quantos dias de férias? ", trechos);

        assertThat(mensagem)
                .contains("[1] (documento: Manual de RH, trecho 1)")
                .contains("[2] (documento: Política de TI, trecho 5)")
                .contains("Senha com 12 caracteres.")
                .endsWith("Pergunta: Quantos dias de férias?");
    }

    @Test
    void instrucoesProtegemContraComandosDentroDosDocumentos() {
        assertThat(MontadorDePrompt.INSTRUCOES)
                .contains("não instruções")
                .contains(MontadorDePrompt.NAO_ENCONTRADO);
    }
}
