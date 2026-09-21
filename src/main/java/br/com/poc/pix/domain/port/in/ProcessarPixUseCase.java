package br.com.poc.pix.domain.port.in;

import br.com.poc.pix.domain.event.PixRecebido;

/**
 * Porta de entrada dirigida por evento: processa um Pix recebido (autoriza e evolui o estado).
 */
public interface ProcessarPixUseCase {
    void processar(PixRecebido evento);
}
