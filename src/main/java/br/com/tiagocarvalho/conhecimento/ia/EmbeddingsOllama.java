package br.com.tiagocarvalho.conhecimento.ia;

import br.com.tiagocarvalho.conhecimento.config.Propriedades;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

/** Gera embeddings com o modelo nomic-embed-text rodando no Ollama (via Spring AI). */
@Component
public class EmbeddingsOllama implements GeradorDeEmbeddings {

    private final OllamaEmbeddingModel modelo;
    private final int dimensoesEsperadas;

    public EmbeddingsOllama(OllamaEmbeddingModel modelo, Propriedades.Rag rag) {
        this.modelo = modelo;
        this.dimensoesEsperadas = rag.dimensoes();
    }

    @Override
    public float[] gerar(String texto) {
        return conferir(modelo.embed(texto));
    }

    @Override
    public List<float[]> gerarVarios(List<String> textos) {
        return modelo.embed(textos).stream().map(this::conferir).toList();
    }

    /** Se alguém trocar o modelo e esquecer da coluna vector(768), o erro aparece aqui com uma mensagem clara. */
    private float[] conferir(float[] vetor) {
        if (vetor.length != dimensoesEsperadas) {
            throw new IllegalStateException("O modelo gerou um vetor de " + vetor.length
                    + " posições, mas o banco espera " + dimensoesEsperadas + " (app.rag.dimensoes).");
        }
        return vetor;
    }
}
