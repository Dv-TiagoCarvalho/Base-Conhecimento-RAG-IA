package br.com.tiagocarvalho.conhecimento.pergunta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache simples, em memória, com tempo de expiração. Evita pagar o Claude de novo por uma pergunta repetida.
 *
 * É esvaziado sempre que um documento é processado ou removido, porque isso pode mudar a resposta certa.
 * Mesma limitação do limitador: é por instância.
 */
public class CacheDeRespostas {

    private record Entrada(RespostaPergunta resposta, Instant expiraEm) {
    }

    private final Duration validade;
    private final int maximoEntradas;
    private final Clock relogio;
    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();

    public CacheDeRespostas(Duration validade, int maximoEntradas, Clock relogio) {
        this.validade = validade;
        this.maximoEntradas = maximoEntradas;
        this.relogio = relogio;
    }

    public Optional<RespostaPergunta> obter(String chave) {
        Entrada entrada = entradas.get(chave);
        if (entrada == null) {
            return Optional.empty();
        }
        if (!relogio.instant().isBefore(entrada.expiraEm())) {
            entradas.remove(chave, entrada);
            return Optional.empty();
        }
        return Optional.of(entrada.resposta());
    }

    public void guardar(String chave, RespostaPergunta resposta) {
        if (entradas.size() >= maximoEntradas) {
            entradas.clear();   // estratégia simples para não crescer sem limite
        }
        entradas.put(chave, new Entrada(resposta, relogio.instant().plus(validade)));
    }

    public void limpar() {
        entradas.clear();
    }

    public int tamanho() {
        return entradas.size();
    }

    /** "  Quantos dias de FÉRIAS?? " e "quantos dias de férias" viram a mesma chave. */
    public static String normalizar(String pergunta) {
        return pergunta.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[?!.]+$", "");
    }
}
