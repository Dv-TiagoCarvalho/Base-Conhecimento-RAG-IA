package br.com.tiagocarvalho.conhecimento.documento;

import java.util.ArrayList;
import java.util.List;

/**
 * Quebra um texto grande em trechos menores ("chunks") para gerar um embedding por trecho.
 *
 * Regras:
 * - respeita os parágrafos: junta parágrafos inteiros até chegar perto do tamanho máximo;
 * - parágrafo maior que o máximo é fatiado em janelas;
 * - trechos vizinhos compartilham um pedaço do final do anterior (sobreposição), para que uma frase
 *   que fica na fronteira entre dois trechos não perca o contexto.
 */
public class DivisorDeTexto {

    private final int tamanhoMaximo;
    private final int sobreposicao;

    public DivisorDeTexto(int tamanhoMaximo, int sobreposicao) {
        if (tamanhoMaximo <= 0) {
            throw new IllegalArgumentException("O tamanho máximo precisa ser positivo.");
        }
        if (sobreposicao < 0 || sobreposicao >= tamanhoMaximo) {
            throw new IllegalArgumentException("A sobreposição precisa estar entre 0 e o tamanho máximo.");
        }
        this.tamanhoMaximo = tamanhoMaximo;
        this.sobreposicao = sobreposicao;
    }

    public List<String> dividir(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String normalizado = texto.replace("\r\n", "\n").replaceAll("[ \\t\\x0B\\f]+", " ").trim();
        String[] paragrafos = normalizado.split("\\n\\s*\\n");

        List<String> trechos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean atualTemConteudoNovo = false;

        for (String bruto : paragrafos) {
            String paragrafo = bruto.trim();
            if (paragrafo.isEmpty()) {
                continue;
            }

            if (paragrafo.length() > tamanhoMaximo) {
                if (atualTemConteudoNovo) {
                    trechos.add(atual.toString());
                }
                atual.setLength(0);
                atualTemConteudoNovo = false;
                trechos.addAll(fatiar(paragrafo));
                continue;
            }

            if (atualTemConteudoNovo && atual.length() + 2 + paragrafo.length() > tamanhoMaximo) {
                String fechado = atual.toString();
                trechos.add(fechado);
                String cauda = cauda(fechado);
                atual.setLength(0);
                if (!cauda.isEmpty() && cauda.length() + 2 + paragrafo.length() <= tamanhoMaximo) {
                    atual.append(cauda);
                }
                atualTemConteudoNovo = false;
            }

            if (!atual.isEmpty()) {
                atual.append("\n\n");
            }
            atual.append(paragrafo);
            atualTemConteudoNovo = true;
        }

        if (atualTemConteudoNovo) {
            trechos.add(atual.toString());
        }
        return trechos;
    }

    /** Fatia um parágrafo longo em janelas de tamanhoMaximo, andando (tamanhoMaximo - sobreposicao) por vez. */
    private List<String> fatiar(String paragrafo) {
        List<String> partes = new ArrayList<>();
        int passo = tamanhoMaximo - sobreposicao;
        for (int inicio = 0; inicio < paragrafo.length(); inicio += passo) {
            int fim = Math.min(paragrafo.length(), inicio + tamanhoMaximo);
            String parte = paragrafo.substring(inicio, fim).trim();
            if (!parte.isEmpty()) {
                partes.add(parte);
            }
            if (fim == paragrafo.length()) {
                break;
            }
        }
        return partes;
    }

    /** Últimos caracteres do trecho, começando numa palavra inteira. */
    private String cauda(String trecho) {
        if (sobreposicao == 0) {
            return "";
        }
        if (trecho.length() <= sobreposicao) {
            return trecho;
        }
        String final_ = trecho.substring(trecho.length() - sobreposicao);
        int espaco = final_.indexOf(' ');
        return (espaco >= 0 ? final_.substring(espaco + 1) : final_).trim();
    }
}
