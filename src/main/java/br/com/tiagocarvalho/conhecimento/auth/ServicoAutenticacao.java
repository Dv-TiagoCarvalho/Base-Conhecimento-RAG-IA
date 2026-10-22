package br.com.tiagocarvalho.conhecimento.auth;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ServicoAutenticacao {

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final ServicoJwt servicoJwt;

    public ServicoAutenticacao(RepositorioUsuarios usuarios, PasswordEncoder codificador, ServicoJwt servicoJwt) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.servicoJwt = servicoJwt;
    }

    /** Todo cadastro feito pela API vira USUARIO. O único ADMIN é criado pelo CriadorDeAdmin. */
    @Transactional
    public DadosAuth.TokenResposta registrar(DadosAuth.Registro registro) {
        String email = normalizar(registro.email());
        if (usuarios.existsByEmail(email)) {
            throw new Excecoes.Conflito("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarios.save(new Usuario(
                registro.nome().trim(), email, codificador.encode(registro.senha()), Papel.USUARIO));
        return resposta(usuario);
    }

    @Transactional(readOnly = true)
    public DadosAuth.TokenResposta entrar(DadosAuth.Entrada entrada) {
        Usuario usuario = usuarios.findByEmail(normalizar(entrada.email()))
                .filter(u -> codificador.matches(entrada.senha(), u.getSenhaHash()))
                .orElseThrow(Excecoes.CredenciaisInvalidas::new);
        return resposta(usuario);
    }

    static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private DadosAuth.TokenResposta resposta(Usuario usuario) {
        return new DadosAuth.TokenResposta(servicoJwt.gerar(usuario), usuario.getNome(), usuario.getEmail(), usuario.getPapel());
    }
}
