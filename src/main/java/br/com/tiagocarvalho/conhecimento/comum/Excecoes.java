package br.com.tiagocarvalho.conhecimento.comum;

/** Exceções de negócio. O TratadorDeErros transforma cada uma no código HTTP certo. */
public final class Excecoes {

    private Excecoes() {
    }

    /** 404 */
    public static class NaoEncontrado extends RuntimeException {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    /** 409 */
    public static class Conflito extends RuntimeException {
        public Conflito(String mensagem) {
            super(mensagem);
        }
    }

    /** 400 */
    public static class RegraDeNegocio extends RuntimeException {
        public RegraDeNegocio(String mensagem) {
            super(mensagem);
        }
    }

    /** 401 */
    public static class CredenciaisInvalidas extends RuntimeException {
        public CredenciaisInvalidas() {
            super("E-mail ou senha incorretos.");
        }
    }

    /** 429 */
    public static class LimiteExcedido extends RuntimeException {
        private final long segundosParaLiberar;

        public LimiteExcedido(long segundosParaLiberar) {
            super("Limite de perguntas por minuto atingido. Tente de novo em " + segundosParaLiberar + " segundos.");
            this.segundosParaLiberar = segundosParaLiberar;
        }

        public long getSegundosParaLiberar() {
            return segundosParaLiberar;
        }
    }
}
