package br.com.tiagocarvalho.conhecimento.pergunta;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Não confia cegamente no modelo. Uma resposta só é aceita se:
 * - não estiver vazia;
 * - citar pelo menos um trecho ([1] ou [Trecho 1]) (ou disser explicitamente que não encontrou a informação);
 * - todas as citações apontarem para trechos que existem ([5] quando só enviamos 3 trechos é invenção).
 */
public final class ValidadorDeResposta {

    private static final Pattern CITACAO = Pattern.compile("\\[(?:trecho\\s*)?(\\d{1,3})]", Pattern.CASE_INSENSITIVE);

    private ValidadorDeResposta() {
    }

    public record Resultado(boolean valida, Set<Integer> citados, boolean naoEncontrado, String motivo) {

        static Resultado invalida(String motivo) {
            return new Resultado(false, Set.of(), false, motivo);
        }
    }

    public static Resultado validar(String resposta, int totalDeTrechos) {
        if (resposta == null || resposta.isBlank()) {
            return Resultado.invalida("resposta vazia");
        }
        // modelos menores às vezes mudam um pouco a frase combinada, por isso a comparação é mais flexível
        if (resposta.toLowerCase(Locale.ROOT).contains("não encontrei")) {
            return new Resultado(true, Set.of(), true, null);
        }

        Set<Integer> citados = new TreeSet<>();
        Matcher m = CITACAO.matcher(resposta);
        while (m.find()) {
            int numero = Integer.parseInt(m.group(1));
            if (numero < 1 || numero > totalDeTrechos) {
                return Resultado.invalida("cita o trecho [" + numero + "], que não existe");
            }
            citados.add(numero);
        }
        if (citados.isEmpty()) {
            return Resultado.invalida("resposta sem indicação de fonte");
        }
        return new Resultado(true, citados, false, null);
    }
}
