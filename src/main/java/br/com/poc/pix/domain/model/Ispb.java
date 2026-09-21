package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

/**
 * ISPB - identificador de 8 digitos da instituicao participante do SPI.
 */
public record Ispb(String valor) {
    public Ispb {
        if (valor == null || !valor.matches("^[0-9]{8}$")) {
            throw new PixInvalidoException("ISPB invalido: esperado 8 digitos");
        }
    }
}
