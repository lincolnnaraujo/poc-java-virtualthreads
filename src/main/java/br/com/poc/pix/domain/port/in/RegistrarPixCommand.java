package br.com.poc.pix.domain.port.in;

import java.math.BigDecimal;

/**
 * Comando de entrada para registrar um Pix. Expresso em tipos simples (fronteira do hexagono);
 * o use case converte para Value Objects de dominio, validando invariantes.
 */
public record RegistrarPixCommand(
        String endToEndId,
        String txid,
        BigDecimal valor,
        String infoEntreClientes,
        Pagador pagador,
        Recebedor recebedor) {

    public record Pagador(
            String nome,
            String cpfCnpj,
            String ispb,
            String agencia,
            String conta,
            String tipoConta) {
    }

    public record Recebedor(
            String nome,
            String cpfCnpj,
            String ispb,
            String agencia,
            String conta,
            String tipoConta,
            String chave,
            String tipoChave) {
    }
}
