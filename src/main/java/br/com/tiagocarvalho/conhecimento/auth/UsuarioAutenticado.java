package br.com.tiagocarvalho.conhecimento.auth;

import java.util.UUID;

/**
 * Quem está fazendo a requisição, montado a partir do token JWT.
 * Os controladores recebem este objeto com @AuthenticationPrincipal.
 */
public record UsuarioAutenticado(UUID id, String email, Papel papel) {
}
