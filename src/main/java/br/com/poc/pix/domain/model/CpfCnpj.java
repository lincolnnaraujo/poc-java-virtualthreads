package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

/**
 * Documento do participante: CPF (11 digitos) ou CNPJ (14 digitos), apenas numeros.
 * Nao valida digito verificador (fora do escopo da POC) - apenas formato.
 */
public record CpfCnpj(String valor) {
    public CpfCnpj {
        if (valor == null || !valor.matches("^[0-9]{11}([0-9]{3})?$")) {
            throw new PixInvalidoException("CPF/CNPJ invalido: esperado 11 (CPF) ou 14 (CNPJ) digitos");
        }
    }
}
