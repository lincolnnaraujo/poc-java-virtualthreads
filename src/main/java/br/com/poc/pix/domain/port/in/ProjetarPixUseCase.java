package br.com.poc.pix.domain.port.in;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;

/** Porta de entrada dirigida por eventos: projeta o ciclo de vida no read model. */
public interface ProjetarPixUseCase {
    void aoReceber(PixRecebido evento);

    void aoProcessar(PixEmProcessamento evento);

    void aoAutorizar(PixAutorizado evento);

    void aoRejeitar(PixRejeitado evento);
}
