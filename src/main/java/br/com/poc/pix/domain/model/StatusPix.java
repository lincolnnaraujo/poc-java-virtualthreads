package br.com.poc.pix.domain.model;

/**
 * Ciclo de vida da transacao Pix.
 * Nesta fatia (002) apenas {@link #RECEBIDO} e produzido; as transicoes seguintes
 * entram nas fatias 003 (processamento) e 004 (projecao de consulta).
 */
public enum StatusPix {
    RECEBIDO,
    EM_PROCESSAMENTO,
    AUTORIZADO,
    REJEITADO,
    DEVOLVIDO
}
