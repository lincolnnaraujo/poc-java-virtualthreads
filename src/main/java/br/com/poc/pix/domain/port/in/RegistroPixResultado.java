package br.com.poc.pix.domain.port.in;

/**
 * Resultado do registro de um Pix. Distingue o caso novo do caso idempotente
 * (mesmo endToEndId ja registrado), permitindo ao adapter web escolher o status HTTP.
 */
public record RegistroPixResultado(String endToEndId, Situacao situacao) {

    public enum Situacao {
        REGISTRADO,
        JA_REGISTRADO
    }

    public static RegistroPixResultado registrado(String endToEndId) {
        return new RegistroPixResultado(endToEndId, Situacao.REGISTRADO);
    }

    public static RegistroPixResultado jaRegistrado(String endToEndId) {
        return new RegistroPixResultado(endToEndId, Situacao.JA_REGISTRADO);
    }
}
