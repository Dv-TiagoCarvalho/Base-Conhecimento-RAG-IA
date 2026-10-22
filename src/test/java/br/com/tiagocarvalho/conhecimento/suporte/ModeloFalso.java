package br.com.tiagocarvalho.conhecimento.suporte;

import br.com.tiagocarvalho.conhecimento.ia.ModeloDeLinguagem;

/** Substitui o Claude nos testes: devolve uma resposta configurável ou simula uma falha. */
public class ModeloFalso implements ModeloDeLinguagem {

    public static final String RESPOSTA_PADRAO = "Cada colaborador tem direito a trinta dias de férias por ano [1].";

    private volatile String resposta = RESPOSTA_PADRAO;
    private volatile boolean falhar;
    private volatile String ultimaMensagem;
    private volatile int chamadas;

    @Override
    public String responder(String instrucoesDeSistema, String mensagemDoUsuario) {
        chamadas++;
        ultimaMensagem = mensagemDoUsuario;
        if (falhar) {
            throw new IllegalStateException("modelo fora do ar (simulado)");
        }
        return resposta;
    }

    public void reiniciar() {
        resposta = RESPOSTA_PADRAO;
        falhar = false;
        ultimaMensagem = null;
        chamadas = 0;
    }

    public void responderCom(String texto) {
        this.resposta = texto;
    }

    public void simularFalha() {
        this.falhar = true;
    }

    public String ultimaMensagem() {
        return ultimaMensagem;
    }

    public int chamadas() {
        return chamadas;
    }
}
