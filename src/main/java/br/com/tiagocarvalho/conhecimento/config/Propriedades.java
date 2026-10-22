package br.com.tiagocarvalho.conhecimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações da aplicação lidas do application.yml (prefixo "app").
 * Cada record corresponde a um bloco do arquivo.
 */
public final class Propriedades {

    private Propriedades() {
    }

    @ConfigurationProperties(prefix = "app.jwt")
    public record Jwt(String segredo, long expiracaoMinutos) {
    }

    @ConfigurationProperties(prefix = "app.admin")
    public record Admin(String nome, String email, String senha) {
    }

    @ConfigurationProperties(prefix = "app.rag")
    public record Rag(int topK, double similaridadeMinima, int dimensoes, int tamanhoTrecho, int sobreposicao) {
    }

    @ConfigurationProperties(prefix = "app.limite")
    public record Limite(int perguntasPorMinuto) {
    }

    @ConfigurationProperties(prefix = "app.cache")
    public record Cache(long ttlMinutos, int maximoEntradas) {
    }
}
