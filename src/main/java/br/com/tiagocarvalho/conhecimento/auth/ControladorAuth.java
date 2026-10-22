package br.com.tiagocarvalho.conhecimento.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ControladorAuth {

    private final ServicoAutenticacao servico;

    public ControladorAuth(ServicoAutenticacao servico) {
        this.servico = servico;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public DadosAuth.TokenResposta registrar(@Valid @RequestBody DadosAuth.Registro registro) {
        return servico.registrar(registro);
    }

    @PostMapping("/entrar")
    public DadosAuth.TokenResposta entrar(@Valid @RequestBody DadosAuth.Entrada entrada) {
        return servico.entrar(entrada);
    }

    @GetMapping("/eu")
    public UsuarioAutenticado eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario;
    }
}
