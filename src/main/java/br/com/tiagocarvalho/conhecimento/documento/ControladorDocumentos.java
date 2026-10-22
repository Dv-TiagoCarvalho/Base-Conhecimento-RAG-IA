package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.auth.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
public class ControladorDocumentos {

    private final ServicoDocumentos servico;

    public ControladorDocumentos(ServicoDocumentos servico) {
        this.servico = servico;
    }

    /** Somente ADMIN (regra no SegurancaConfig). Responde 201 com status PENDENTE. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoResposta enviar(@RequestParam("arquivo") MultipartFile arquivo,
                                    @RequestParam(value = "titulo", required = false) String titulo,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.enviar(titulo, arquivo, usuario.id());
    }

    @GetMapping
    public List<DocumentoResposta> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public DocumentoResposta buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    /** Somente ADMIN. Volta o documento para a fila (útil quando falhou). */
    @PostMapping("/{id}/reprocessar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentoResposta reprocessar(@PathVariable UUID id) {
        return servico.reprocessar(id);
    }

    /** Somente ADMIN. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        servico.remover(id);
    }
}
