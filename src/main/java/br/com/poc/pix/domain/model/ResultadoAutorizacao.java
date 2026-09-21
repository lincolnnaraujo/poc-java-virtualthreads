package br.com.poc.pix.domain.model;

/**
 * Resultado de uma tentativa de autorizacao junto ao SPI.
 */
public record ResultadoAutorizacao(boolean autorizado, String motivo) {

    public static ResultadoAutorizacao aprovada() {
        return new ResultadoAutorizacao(true, null);
    }

    public static ResultadoAutorizacao rejeitada(String motivo) {
        return new ResultadoAutorizacao(false, motivo);
    }
}
