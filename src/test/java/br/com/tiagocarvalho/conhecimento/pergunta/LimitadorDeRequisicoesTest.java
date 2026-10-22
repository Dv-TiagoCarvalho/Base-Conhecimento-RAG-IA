package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;
import br.com.tiagocarvalho.conhecimento.suporte.RelogioAjustavel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorDeRequisicoesTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-01-01T10:00:00Z"));
    private final LimitadorDeRequisicoes limitador = new LimitadorDeRequisicoes(2, Duration.ofMinutes(1), relogio);

    @Test
    void recusaQuandoPassaDoLimiteDentroDaJanela() {
        limitador.verificar("ana");
        limitador.verificar("ana");

        assertThatThrownBy(() -> limitador.verificar("ana"))
                .isInstanceOf(Excecoes.LimiteExcedido.class)
                .hasMessageContaining("60 segundos");
    }

    @Test
    void liberaDepoisQueAJanelaPassa() {
        limitador.verificar("ana");
        limitador.verificar("ana");
        relogio.avancar(Duration.ofSeconds(61));

        assertThatCode(() -> limitador.verificar("ana")).doesNotThrowAnyException();
    }

    @Test
    void cadaUsuarioTemSeuProprioLimite() {
        limitador.verificar("ana");
        limitador.verificar("ana");

        assertThatCode(() -> limitador.verificar("bruno")).doesNotThrowAnyException();
    }
}
