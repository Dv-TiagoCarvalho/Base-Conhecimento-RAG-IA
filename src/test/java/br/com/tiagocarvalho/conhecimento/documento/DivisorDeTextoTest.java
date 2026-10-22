package br.com.tiagocarvalho.conhecimento.documento;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DivisorDeTextoTest {

    private final DivisorDeTexto divisor = new DivisorDeTexto(100, 20);

    @Test
    void textoVazioNaoGeraTrechos() {
        assertThat(divisor.dividir(null)).isEmpty();
        assertThat(divisor.dividir("   \n\n  ")).isEmpty();
    }

    @Test
    void textoCurtoViraUmTrechoSo() {
        assertThat(divisor.dividir("Primeiro parágrafo.\n\nSegundo parágrafo."))
                .containsExactly("Primeiro parágrafo.\n\nSegundo parágrafo.");
    }

    @Test
    void paragrafosQueNaoCabemJuntosVaoParaTrechosDiferentes() {
        String p1 = "a".repeat(60);
        String p2 = "b".repeat(60);
        List<String> trechos = divisor.dividir(p1 + "\n\n" + p2);

        assertThat(trechos).hasSize(2);
        assertThat(trechos.get(0)).isEqualTo(p1);
        assertThat(trechos.get(1)).endsWith(p2);
    }

    @Test
    void nenhumTrechoPassaDoTamanhoMaximo() {
        String texto = String.join("\n\n", "palavra ".repeat(10), "outra coisa ".repeat(30), "fim ".repeat(5));
        assertThat(divisor.dividir(texto)).allSatisfy(t -> assertThat(t.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void paragrafoLongoEhFatiadoComSobreposicao() {
        String longo = "0123456789".repeat(25);   // 250 caracteres, sem espaços
        List<String> partes = divisor.dividir(longo);

        // passo de 80 (100 - 20): começa em 0, 80, 160 -> 3 partes
        assertThat(partes).hasSize(3);
        assertThat(partes.get(0)).hasSize(100);
        // os 20 últimos caracteres de uma parte são os 20 primeiros da seguinte
        assertThat(partes.get(1)).startsWith(partes.get(0).substring(80));
    }

    @Test
    void trechoSeguinteComecaComOFinalDoAnterior() {
        String p1 = "Primeira frase do texto com algumas palavras para ocupar espaço suficiente aqui.";
        String p2 = "Segunda frase que não cabe junto com a primeira dentro do mesmo trecho.";
        List<String> trechos = divisor.dividir(p1 + "\n\n" + p2);

        assertThat(trechos).hasSize(2);
        assertThat(trechos.get(1)).contains("suficiente aqui.").endsWith(p2);
    }

    @Test
    void configuracaoInvalidaEhRecusada() {
        assertThatThrownBy(() -> new DivisorDeTexto(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DivisorDeTexto(100, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}
