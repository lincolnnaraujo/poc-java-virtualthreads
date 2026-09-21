package br.com.poc.pix.domain.model;

/**
 * Tipo de conta (padrao ISO 20022 usado no Pix).
 * CACC=corrente, SVGS=poupanca, SLRY=salario, TRAN=pagamento.
 */
public enum TipoConta {
    CACC,
    SVGS,
    SLRY,
    TRAN
}
