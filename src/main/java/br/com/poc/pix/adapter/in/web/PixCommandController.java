package br.com.poc.pix.adapter.in.web;

import br.com.poc.pix.adapter.in.web.dto.RegistrarPixRequest;
import br.com.poc.pix.adapter.in.web.dto.RegistrarPixResponse;
import br.com.poc.pix.common.Mascaras;
import br.com.poc.pix.domain.port.in.RegistrarPixUseCase;
import br.com.poc.pix.domain.port.in.RegistroPixResultado;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Adapter de entrada HTTP para o comando de registro de Pix.
 * Responde 202 (novo) ou 200 (idempotente, ja registrado).
 */
@RestController
@RequestMapping("/v1/pix")
public class PixCommandController {

    private static final Logger log = LoggerFactory.getLogger(PixCommandController.class);

    private final RegistrarPixUseCase registrarPix;

    public PixCommandController(RegistrarPixUseCase registrarPix) {
        this.registrarPix = registrarPix;
    }

    @PostMapping
    public ResponseEntity<RegistrarPixResponse> registrar(@Valid @RequestBody RegistrarPixRequest request) {
        // PII mascarada (constitution P5): nunca logar CPF/CNPJ ou chave em texto claro.
        log.info("POST /v1/pix endToEndId={} pagadorDoc={} recebedorChave={}",
                request.endToEndId(),
                Mascaras.documento(request.pagador().cpfCnpj()),
                Mascaras.chave(request.recebedor().chave()));

        RegistroPixResultado resultado = registrarPix.registrar(RegistrarPixRequestMapper.toCommand(request));
        URI location = URI.create("/v1/pix/" + resultado.endToEndId());

        if (resultado.situacao() == RegistroPixResultado.Situacao.JA_REGISTRADO) {
            return ResponseEntity.ok()
                    .location(location)
                    .body(new RegistrarPixResponse(resultado.endToEndId(), "JA_REGISTRADO",
                            "Pix ja registrado (resposta idempotente)"));
        }

        return ResponseEntity.accepted()
                .location(location)
                .body(new RegistrarPixResponse(resultado.endToEndId(), "RECEBIDO",
                        "Pix aceito para processamento assincrono"));
    }
}
