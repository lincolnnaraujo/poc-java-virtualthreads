package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

/**
 * Chave Pix do recebedor (max 77 caracteres, conforme dominio Pix).
 */
public record ChavePix(String valor) {
    public ChavePix {
        if (valor == null || valor.isBlank() || valor.length() > 77) {
            throw new PixInvalidoException("chave Pix invalida: obrigatoria e com no maximo 77 caracteres");
        }
    }
}
