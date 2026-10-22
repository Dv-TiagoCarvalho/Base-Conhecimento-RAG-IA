package br.com.tiagocarvalho.conhecimento.suporte;

import br.com.tiagocarvalho.conhecimento.ia.GeradorDeEmbeddings;

import java.util.Locale;

/**
 * Gerador de embeddings determinístico para testes, sem depender do Ollama.
 * Cada palavra (com 3+ letras) soma 1 numa posição do vetor escolhida pelo hash da palavra.
 * Resultado: textos que compartilham palavras ficam próximos, textos sem palavras em comum ficam distantes.
 */
public class EmbeddingsFalsos implements GeradorDeEmbeddings {

    private final int dimensoes;

    public EmbeddingsFalsos(int dimensoes) {
        this.dimensoes = dimensoes;
    }

    @Override
    public float[] gerar(String texto) {
        float[] vetor = new float[dimensoes];
        for (String palavra : texto.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (palavra.length() >= 3) {
                vetor[Math.floorMod(palavra.hashCode(), dimensoes)] += 1f;
            }
        }
        double norma = 0;
        for (float v : vetor) {
            norma += v * v;
        }
        if (norma == 0) {
            vetor[dimensoes - 1] = 1f;   // evita vetor zerado (distância de cosseno indefinida)
            return vetor;
        }
        float raiz = (float) Math.sqrt(norma);
        for (int i = 0; i < dimensoes; i++) {
            vetor[i] /= raiz;
        }
        return vetor;
    }
}
