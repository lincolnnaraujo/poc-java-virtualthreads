package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetario do Pix. Invariantes: maior que zero e no maximo 2 casas decimais.
 */
public record Valor(BigDecimal quantia) {
    public Valor {
        if (quantia == null || quantia.signum() <= 0) {
            throw new PixInvalidoException("valor deve ser maior que zero");
        }
        if (quantia.scale() > 2) {
            throw new PixInvalidoException("valor deve ter no maximo 2 casas decimais");
        }
        quantia = quantia.setScale(2, RoundingMode.UNNECESSARY);
    }
}
