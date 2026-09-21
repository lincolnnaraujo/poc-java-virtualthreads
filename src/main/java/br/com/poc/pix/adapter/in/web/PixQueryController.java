package br.com.poc.pix.adapter.in.web;

import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.port.in.ConsultarPixUseCase;
import br.com.poc.pix.domain.port.out.ConsultaPixView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada HTTP para consulta do ciclo de vida do Pix (lado de leitura do CQRS).
 */
@RestController
@RequestMapping("/v1/pix")
public class PixQueryController {

    private final ConsultarPixUseCase consultarPix;

    public PixQueryController(ConsultarPixUseCase consultarPix) {
        this.consultarPix = consultarPix;
    }

    @GetMapping("/{endToEndId}")
    public ConsultaPixView consultar(@PathVariable String endToEndId) {
        // EndToEndId invalido -> PixInvalidoException (400); inexistente -> PixNaoEncontradoException (404).
        return consultarPix.consultar(new EndToEndId(endToEndId));
    }
}
