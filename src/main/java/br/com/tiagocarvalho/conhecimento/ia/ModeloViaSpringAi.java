package br.com.tiagocarvalho.conhecimento.ia;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Lógica comum aos modelos chamados pelo Spring AI (Ollama e Claude). Cada subclasse só escolhe qual ChatModel usar.
 *
 * As mensagens são montadas diretamente, sem template, porque o texto dos documentos pode conter
 * chaves { } que um motor de template tentaria interpretar.
 */
abstract class ModeloViaSpringAi implements ModeloDeLinguagem {

    private final ChatModel chatModel;
    private final String nome;

    protected ModeloViaSpringAi(ChatModel chatModel, String nome) {
        this.chatModel = chatModel;
        this.nome = nome;
    }

    @Override
    public String responder(String instrucoesDeSistema, String mensagemDoUsuario) {
        Prompt prompt = new Prompt(List.of(new SystemMessage(instrucoesDeSistema), new UserMessage(mensagemDoUsuario)));
        ChatResponse resposta = chatModel.call(prompt);
        if (resposta == null || resposta.getResult() == null || resposta.getResult().getOutput() == null) {
            throw new IllegalStateException("O modelo " + nome + " não retornou conteúdo.");
        }
        return resposta.getResult().getOutput().getText();
    }
}
