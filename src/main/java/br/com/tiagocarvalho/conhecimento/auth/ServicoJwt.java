package br.com.tiagocarvalho.conhecimento.auth;

import br.com.tiagocarvalho.conhecimento.config.Propriedades;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Gera e valida tokens JWT assinados com HMAC-SHA256. */
@Service
public class ServicoJwt {

    private final SecretKey chave;
    private final Duration validade;

    public ServicoJwt(Propriedades.Jwt config) {
        byte[] bytes = config.segredo() == null ? new byte[0] : config.segredo().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.segredo precisa ter pelo menos 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.validade = Duration.ofMinutes(config.expiracaoMinutos());
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("papel", usuario.getPapel().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    /** Retorna vazio se o token for inválido, adulterado ou estiver expirado. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims dados = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UsuarioAutenticado(
                    UUID.fromString(dados.getSubject()),
                    dados.get("email", String.class),
                    Papel.valueOf(dados.get("papel", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
