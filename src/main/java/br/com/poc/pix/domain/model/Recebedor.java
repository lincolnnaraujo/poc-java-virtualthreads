package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

/**
 * Participante recebedor da transacao Pix (inclui a chave Pix).
 */
public record Recebedor(
        String nome,
        CpfCnpj cpfCnpj,
        Ispb ispb,
        String agencia,
        String conta,
        TipoConta tipoConta,
        ChavePix chave,
        TipoChave tipoChave) {

    public Recebedor {
        if (nome == null || nome.isBlank()) {
            throw new PixInvalidoException("recebedor.nome e obrigatorio");
        }
        if (cpfCnpj == null || ispb == null || tipoConta == null) {
            throw new PixInvalidoException("recebedor incompleto (cpfCnpj, ispb e tipoConta sao obrigatorios)");
        }
        if (conta == null || conta.isBlank()) {
            throw new PixInvalidoException("recebedor.conta e obrigatoria");
        }
        if (chave == null || tipoChave == null) {
            throw new PixInvalidoException("recebedor.chave e recebedor.tipoChave sao obrigatorios");
        }
    }
}
