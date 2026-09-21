package br.com.poc.pix.domain;

/**
 * Violacao de invariante de dominio do Pix (dados invalidos/incompletos).
 * Traduzida para HTTP 400 pelo adapter web.
 */
public class PixInvalidoException extends RuntimeException {
    public PixInvalidoException(String mensagem) {
        super(mensagem);
    }
}
