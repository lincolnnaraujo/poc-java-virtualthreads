package br.com.poc.pix.domain.port.out;

import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.StatusPix;
import br.com.poc.pix.domain.model.TransacaoPix;

import java.util.Optional;

/**
 * Porta de saida de persistencia do modelo de escrita.
 */
public interface PixRepositoryPort {

    /** Retorna true se ja existe transacao com o endToEndId informado. */
    boolean existe(EndToEndId endToEndId);

    /**
     * Persiste a transacao (tabelas normalizadas) de forma atomica.
     *
     * @throws br.com.poc.pix.domain.PixJaRegistradoException se o endToEndId ja existir
     *         (violacao da UNIQUE constraint) - suporte a idempotencia sob concorrencia.
     */
    void salvar(TransacaoPix transacao);

    /** Status atual da transacao, ou vazio se ela nao existir. */
    Optional<StatusPix> statusAtual(EndToEndId endToEndId);

    /** Evolui o status da transacao (registra transicao no historico). */
    void atualizarStatus(EndToEndId endToEndId, StatusPix novoStatus);

    /** Evolui o status com um motivo (ex: rejeicao). */
    void atualizarStatus(EndToEndId endToEndId, StatusPix novoStatus, String motivo);
}
