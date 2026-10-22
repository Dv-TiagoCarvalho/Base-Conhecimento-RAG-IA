package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.suporte.RelogioAjustavel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheDeRespostasTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-01-01T10:00:00Z"));
    private final CacheDeRespostas cache = new CacheDeRespostas(Duration.ofMinutes(10), 3, relogio);
    private final RespostaPergunta resposta =
            new RespostaPergunta("30 dias [1]", RespostaPergunta.Modo.COMPLETA, List.of(), false);

    @Test
    void devolveOQueFoiGuardado() {
        cache.guardar("chave", resposta);
        assertThat(cache.obter("chave")).contains(resposta);
    }

    @Test
    void expiraDepoisDoTempoDeValidade() {
        cache.guardar("chave", resposta);
        relogio.avancar(Duration.ofMinutes(10));
        assertThat(cache.obter("chave")).isEmpty();
    }

    @Test
    void naoCresceAlemDoMaximo() {
        cache.guardar("a", resposta);
        cache.guardar("b", resposta);
        cache.guardar("c", resposta);
        cache.guardar("d", resposta);
        assertThat(cache.tamanho()).isLessThanOrEqualTo(3);
    }

    @Test
    void perguntasEquivalentesGeramAMesmaChave() {
        assertThat(CacheDeRespostas.normalizar("  Quantos dias de   FÉRIAS?? "))
                .isEqualTo(CacheDeRespostas.normalizar("quantos dias de férias"));
    }
}
