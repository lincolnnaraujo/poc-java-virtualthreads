package br.com.poc.pix.application;

import br.com.poc.pix.domain.PixNaoEncontradoException;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.port.in.ConsultarPixUseCase;
import br.com.poc.pix.domain.port.out.ConsultaPixReadPort;
import br.com.poc.pix.domain.port.out.ConsultaPixView;
import org.springframework.stereotype.Service;

@Service
public class ConsultarPixService implements ConsultarPixUseCase {

    private final ConsultaPixReadPort leitura;

    public ConsultarPixService(ConsultaPixReadPort leitura) {
        this.leitura = leitura;
    }

    @Override
    public ConsultaPixView consultar(EndToEndId endToEndId) {
        return leitura.porEndToEndId(endToEndId)
                .orElseThrow(() -> new PixNaoEncontradoException(endToEndId.valor()));
    }
}
