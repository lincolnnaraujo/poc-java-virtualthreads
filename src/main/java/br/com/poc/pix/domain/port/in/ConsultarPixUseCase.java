package br.com.poc.pix.domain.port.in;

import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.port.out.ConsultaPixView;

/** Porta de entrada: consultar o ciclo de vida de um Pix. */
public interface ConsultarPixUseCase {
    /**
     * @throws br.com.poc.pix.domain.PixNaoEncontradoException se o endToEndId nao existir no read model.
     */
    ConsultaPixView consultar(EndToEndId endToEndId);
}
