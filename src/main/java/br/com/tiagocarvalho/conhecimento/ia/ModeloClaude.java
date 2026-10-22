package br.com.tiagocarvalho.conhecimento.ia;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Claude pela API da Anthropic (pago por uso). Ativo só quando IA_PROVEDOR=anthropic.
 */
@Component
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "anthropic")
public class ModeloClaude extends ModeloViaSpringAi {

    public ModeloClaude(AnthropicChatModel chatModel) {
        super(chatModel, "Claude");
    }
}
