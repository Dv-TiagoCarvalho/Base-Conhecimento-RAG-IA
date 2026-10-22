package br.com.tiagocarvalho.conhecimento.ia;

/**
 * Gera uma resposta em texto a partir de instruções (mensagem de sistema) e da mensagem do usuário.
 * A implementação real usa o Claude; nos testes, uma implementação falsa.
 */
public interface ModeloDeLinguagem {

    String responder(String instrucoesDeSistema, String mensagemDoUsuario);
}
