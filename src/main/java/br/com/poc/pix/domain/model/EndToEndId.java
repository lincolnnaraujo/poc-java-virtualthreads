package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

import java.util.regex.Pattern;

/**
 * Identificador fim-a-fim do Pix (chave de idempotencia - constitution P4).
 * Formato: 'E' + 31 caracteres alfanumericos (ISPB + timestamp + sequencial), total 32.
 */
public record EndToEndId(String valor) {

    private static final Pattern PADRAO = Pattern.compile("^E[0-9A-Za-z]{31}$");

    public EndToEndId {
        if (valor == null || !PADRAO.matcher(valor).matches()) {
            throw new PixInvalidoException(
                    "endToEndId invalido: esperado 'E' seguido de 31 caracteres alfanumericos (32 no total)");
        }
    }
}
