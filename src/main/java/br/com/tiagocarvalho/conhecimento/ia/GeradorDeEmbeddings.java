package br.com.tiagocarvalho.conhecimento.ia;

import java.util.List;

/**
 * Transforma texto em vetor numérico (embedding). Textos com significado parecido geram vetores próximos.
 *
 * O resto do sistema depende só desta interface, e não do Ollama diretamente. Isso permite trocar
 * o provedor sem mexer na regra de negócio e usar uma versão falsa nos testes.
 */
public interface GeradorDeEmbeddings {

    float[] gerar(String texto);

    default List<float[]> gerarVarios(List<String> textos) {
        return textos.stream().map(this::gerar).toList();
    }
}
