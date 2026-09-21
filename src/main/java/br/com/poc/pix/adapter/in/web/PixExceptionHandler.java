package br.com.poc.pix.adapter.in.web;

import br.com.poc.pix.domain.PixInvalidoException;
import br.com.poc.pix.domain.PixNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tradutor de erros para respostas RFC 7807 (application/problem+json).
 */
@RestControllerAdvice
public class PixExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PixExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail aoFalharValidacao(MethodArgumentNotValidException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos sao invalidos");
        problema.setTitle("Requisicao invalida");

        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> erros.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(PixInvalidoException.class)
    public ProblemDetail aoRejeitarDominio(PixInvalidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Pix invalido");
        return problema;
    }

    @ExceptionHandler(PixNaoEncontradoException.class)
    public ProblemDetail aoNaoEncontrar(PixNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Pix nao encontrado");
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail aoFalharInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisicao", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
        problema.setTitle("Erro interno");
        return problema;
    }
}
