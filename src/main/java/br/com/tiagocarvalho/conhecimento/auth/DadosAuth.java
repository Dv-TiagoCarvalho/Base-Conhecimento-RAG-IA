package br.com.tiagocarvalho.conhecimento.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formatos de entrada e saída das rotas de autenticação. */
public final class DadosAuth {

    private DadosAuth() {
    }

    public record Registro(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 255) String email,
            // 72 é o limite de bytes que o BCrypt considera
            @NotBlank @Size(min = 8, max = 72) String senha) {
    }

    public record Entrada(
            @NotBlank @Email String email,
            @NotBlank String senha) {
    }

    public record TokenResposta(String token, String nome, String email, Papel papel) {
    }
}
