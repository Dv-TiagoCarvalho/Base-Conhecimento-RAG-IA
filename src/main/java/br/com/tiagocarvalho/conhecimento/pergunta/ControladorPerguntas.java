package br.com.tiagocarvalho.conhecimento.pergunta;

import br.com.tiagocarvalho.conhecimento.auth.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perguntas")
public class ControladorPerguntas {

    public record PerguntaRequisicao(@NotBlank @Size(max = 1000) String pergunta) {
    }

    private final ServicoPerguntas servico;

    public ControladorPerguntas(ServicoPerguntas servico) {
        this.servico = servico;
    }

    @PostMapping
    public RespostaPergunta perguntar(@Valid @RequestBody PerguntaRequisicao requisicao,
                                      @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.perguntar(requisicao.pergunta(), usuario);
    }
}
