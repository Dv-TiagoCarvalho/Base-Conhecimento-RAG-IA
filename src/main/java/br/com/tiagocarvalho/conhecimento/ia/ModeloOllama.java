package br.com.tiagocarvalho.conhecimento.ia;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Modelo local e gratuito, rodando no Ollama (padrão: qwen2.5:3b).
 * Ativo quando IA_PROVEDOR=ollama, que é o valor padrão.
 */
@Component
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "ollama", matchIfMissing = true)
public class ModeloOllama extends ModeloViaSpringAi {

    public ModeloOllama(OllamaChatModel chatModel) {
        super(chatModel, "Ollama");
    }
}
