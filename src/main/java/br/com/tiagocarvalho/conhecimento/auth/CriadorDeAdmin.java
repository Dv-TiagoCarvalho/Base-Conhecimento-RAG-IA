package br.com.tiagocarvalho.conhecimento.auth;

import br.com.tiagocarvalho.conhecimento.config.Propriedades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Na inicialização, cria o administrador definido em ADMIN_EMAIL / ADMIN_SENHA, se ele ainda não existir.
 * Assim nenhuma senha de admin fica escrita no código.
 */
@Component
public class CriadorDeAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriadorDeAdmin.class);

    private final Propriedades.Admin config;
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;

    public CriadorDeAdmin(Propriedades.Admin config, RepositorioUsuarios usuarios, PasswordEncoder codificador) {
        this.config = config;
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (config.email() == null || config.email().isBlank() || config.senha() == null || config.senha().isBlank()) {
            log.warn("ADMIN_EMAIL/ADMIN_SENHA não definidos: nenhum administrador foi criado.");
            return;
        }
        String email = ServicoAutenticacao.normalizar(config.email());
        if (usuarios.existsByEmail(email)) {
            return;
        }
        usuarios.save(new Usuario(config.nome(), email, codificador.encode(config.senha()), Papel.ADMIN));
        log.info("Administrador {} criado.", email);
    }
}
