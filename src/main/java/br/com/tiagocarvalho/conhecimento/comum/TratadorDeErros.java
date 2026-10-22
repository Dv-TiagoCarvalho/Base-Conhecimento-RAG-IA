package br.com.tiagocarvalho.conhecimento.comum;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Converte exceções em respostas no formato ProblemDetail (RFC 9457), o padrão do Spring para erros de API.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(Excecoes.NaoEncontrado.class)
    public ProblemDetail naoEncontrado(Excecoes.NaoEncontrado e) {
        return problema(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(Excecoes.Conflito.class)
    public ProblemDetail conflito(Excecoes.Conflito e) {
        return problema(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Excecoes.RegraDeNegocio.class)
    public ProblemDetail regraDeNegocio(Excecoes.RegraDeNegocio e) {
        return problema(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Excecoes.CredenciaisInvalidas.class)
    public ProblemDetail credenciais(Excecoes.CredenciaisInvalidas e) {
        return problema(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(Excecoes.LimiteExcedido.class)
    public ResponseEntity<ProblemDetail> limite(Excecoes.LimiteExcedido e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getSegundosParaLiberar()))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        String detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, detalhes);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail arquivoGrande(MaxUploadSizeExceededException e) {
        return problema(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo passa do limite de 10 MB.");
    }

    private ProblemDetail problema(HttpStatus status, String detalhe) {
        return ProblemDetail.forStatusAndDetail(status, detalhe);
    }
}
