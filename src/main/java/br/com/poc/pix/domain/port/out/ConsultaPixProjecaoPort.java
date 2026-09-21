package br.com.poc.pix.domain.port.out;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;

/**
 * Porta de saida de escrita do read model (projecao). Cada metodo aplica um upsert idempotente
 * correspondente a uma etapa do ciclo de vida.
 */
public interface ConsultaPixProjecaoPort {
    void aoReceber(PixRecebido evento);

    void aoProcessar(PixEmProcessamento evento);

    void aoAutorizar(PixAutorizado evento);

    void aoRejeitar(PixRejeitado evento);
}
