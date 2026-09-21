package br.com.poc.pix.application;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;
import br.com.poc.pix.domain.port.in.ProjetarPixUseCase;
import br.com.poc.pix.domain.port.out.ConsultaPixProjecaoPort;
import org.springframework.stereotype.Service;

/**
 * Aplica os eventos de dominio ao read model (projecao CQRS). Delega ao port de projecao,
 * cujos upserts sao idempotentes e tolerantes a ordem (constitution: consistencia eventual).
 */
@Service
public class ProjecaoPixService implements ProjetarPixUseCase {

    private final ConsultaPixProjecaoPort projecao;

    public ProjecaoPixService(ConsultaPixProjecaoPort projecao) {
        this.projecao = projecao;
    }

    @Override
    public void aoReceber(PixRecebido evento) {
        projecao.aoReceber(evento);
    }

    @Override
    public void aoProcessar(PixEmProcessamento evento) {
        projecao.aoProcessar(evento);
    }

    @Override
    public void aoAutorizar(PixAutorizado evento) {
        projecao.aoAutorizar(evento);
    }

    @Override
    public void aoRejeitar(PixRejeitado evento) {
        projecao.aoRejeitar(evento);
    }
}
