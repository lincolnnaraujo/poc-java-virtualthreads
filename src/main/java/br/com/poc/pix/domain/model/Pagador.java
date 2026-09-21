package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

/**
 * Participante pagador da transacao Pix.
 */
public record Pagador(
        String nome,
        CpfCnpj cpfCnpj,
        Ispb ispb,
        String agencia,
        String conta,
        TipoConta tipoConta) {

    public Pagador {
        if (nome == null || nome.isBlank()) {
            throw new PixInvalidoException("pagador.nome e obrigatorio");
        }
        if (cpfCnpj == null || ispb == null || tipoConta == null) {
            throw new PixInvalidoException("pagador incompleto (cpfCnpj, ispb e tipoConta sao obrigatorios)");
        }
        if (conta == null || conta.isBlank()) {
            throw new PixInvalidoException("pagador.conta e obrigatoria");
        }
    }
}
