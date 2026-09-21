package br.com.poc.pix.domain;

/**
 * Indica que um Pix com o mesmo endToEndId ja foi registrado (idempotencia - constitution P4).
 * Lancada pelo adapter de persistencia ao detectar violacao da UNIQUE constraint;
 * tratada pelo use case como caso idempotente (nao e erro).
 */
public class PixJaRegistradoException extends RuntimeException {
    private final String endToEndId;

    public PixJaRegistradoException(String endToEndId) {
        super("Pix ja registrado: endToEndId=" + endToEndId);
        this.endToEndId = endToEndId;
    }

    public String endToEndId() {
        return endToEndId;
    }
}
