package br.com.tiagocarvalho.conhecimento.pergunta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorDeRespostaTest {

    @Test
    void aceitaRespostaComCitacoesValidas() {
        var r = ValidadorDeResposta.validar("São 30 dias [1], divididos em até 3 períodos [3].", 3);
        assertThat(r.valida()).isTrue();
        assertThat(r.citados()).containsExactly(1, 3);
    }

    @Test
    void aceitaCitacaoNoFormatoTrechoN() {
        var r = ValidadorDeResposta.validar("São 30 dias [Trecho 2].", 3);
        assertThat(r.valida()).isTrue();
        assertThat(r.citados()).containsExactly(2);
    }

    @Test
    void recusaCitacaoDeTrechoQueNaoExiste() {
        var r = ValidadorDeResposta.validar("Segundo o trecho [5], são 30 dias.", 3);
        assertThat(r.valida()).isFalse();
        assertThat(r.motivo()).contains("[5]");
    }

    @Test
    void recusaRespostaSemFonte() {
        assertThat(ValidadorDeResposta.validar("São 30 dias.", 3).valida()).isFalse();
    }

    @Test
    void recusaRespostaVazia() {
        assertThat(ValidadorDeResposta.validar("  ", 3).valida()).isFalse();
        assertThat(ValidadorDeResposta.validar(null, 3).valida()).isFalse();
    }

    @Test
    void aceitaQuandoOModeloDizQueNaoEncontrou() {
        var r = ValidadorDeResposta.validar(MontadorDePrompt.NAO_ENCONTRADO, 3);
        assertThat(r.valida()).isTrue();
        assertThat(r.naoEncontrado()).isTrue();
    }
}
