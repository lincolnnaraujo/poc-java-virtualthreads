package br.com.poc.spisim.web;

import br.com.poc.spisim.service.AutorizadorService;
import br.com.poc.spisim.service.AutorizarResultado;
import br.com.poc.spisim.web.dto.AutorizarRequest;
import br.com.poc.spisim.web.dto.AutorizarResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de autorizacao do SPI simulado.
 */
@RestController
public class AutorizadorController {

    private final AutorizadorService service;

    public AutorizadorController(AutorizadorService service) {
        this.service = service;
    }

    @PostMapping("/spi/autorizar")
    public AutorizarResponse autorizar(@RequestBody(required = false) AutorizarRequest request) {
        String endToEndId = request == null ? null : request.endToEndId();
        AutorizarResultado resultado = service.autorizar(endToEndId);
        return new AutorizarResponse(endToEndId, resultado.decisao().name(), resultado.latenciaMs());
    }
}
