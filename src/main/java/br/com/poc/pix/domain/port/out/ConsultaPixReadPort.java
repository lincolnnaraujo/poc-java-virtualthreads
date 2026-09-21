package br.com.poc.pix.domain.port.out;

import br.com.poc.pix.domain.model.EndToEndId;

import java.util.Optional;

/** Porta de saida de leitura do read model de consulta. */
public interface ConsultaPixReadPort {
    Optional<ConsultaPixView> porEndToEndId(EndToEndId endToEndId);
}
