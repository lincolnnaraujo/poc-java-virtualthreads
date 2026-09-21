package br.com.poc.pix.domain;

/** Consulta de um endToEndId inexistente no read model (traduzida para HTTP 404). */
public class PixNaoEncontradoException extends RuntimeException {
    public PixNaoEncontradoException(String endToEndId) {
        super("Pix nao encontrado: " + endToEndId);
    }
}
