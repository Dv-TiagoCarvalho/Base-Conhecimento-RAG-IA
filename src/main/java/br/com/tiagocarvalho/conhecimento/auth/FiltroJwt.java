package br.com.tiagocarvalho.conhecimento.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o cabeçalho "Authorization: Bearer <token>". Se o token for válido, informa ao Spring Security
 * quem é o usuário e qual o papel dele. Se não houver token, segue sem autenticar
 * e as regras do SegurancaConfig decidem se a rota exige login.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final ServicoJwt servicoJwt;

    public FiltroJwt(ServicoJwt servicoJwt) {
        this.servicoJwt = servicoJwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            servicoJwt.validar(cabecalho.substring(PREFIXO.length())).ifPresent(usuario -> {
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.papel().name())));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            });
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
