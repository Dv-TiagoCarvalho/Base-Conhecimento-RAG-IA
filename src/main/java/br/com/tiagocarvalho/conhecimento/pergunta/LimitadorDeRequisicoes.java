package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de requisições por usuário com janela deslizante.
 * Guarda o horário das últimas chamadas de cada usuário e recusa quando há "limite" chamadas
 * dentro da janela. Protege o custo da API do Claude contra abuso.
 *
 * Limitação conhecida: fica na memória de cada instância. Com várias instâncias, o certo
 * seria guardar isso num lugar compartilhado (por exemplo, Redis).
 */
public class LimitadorDeRequisicoes {

    private final int limite;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> historico = new ConcurrentHashMap<>();

    public LimitadorDeRequisicoes(int limite, Duration janela, Clock relogio) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite precisa ser positivo.");
        }
        this.limite = limite;
        this.janela = janela;
        this.relogio = relogio;
    }

    public void verificar(String chave) {
        Instant agora = relogio.instant();
        Instant inicioDaJanela = agora.minus(janela);
        Deque<Instant> chamadas = historico.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (chamadas) {
            while (!chamadas.isEmpty() && !chamadas.peekFirst().isAfter(inicioDaJanela)) {
                chamadas.pollFirst();
            }
            if (chamadas.size() >= limite) {
                Instant libera = chamadas.peekFirst().plus(janela);
                long segundos = Math.max(1, Duration.between(agora, libera).toSeconds());
                throw new Excecoes.LimiteExcedido(segundos);
            }
            chamadas.addLast(agora);
        }
    }
}
