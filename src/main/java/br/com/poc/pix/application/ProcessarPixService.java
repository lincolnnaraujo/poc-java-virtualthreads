package br.com.poc.pix.application;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.ResultadoAutorizacao;
import br.com.poc.pix.domain.model.StatusPix;
import br.com.poc.pix.domain.model.Valor;
import br.com.poc.pix.domain.port.in.ProcessarPixUseCase;
import br.com.poc.pix.domain.port.out.AutorizadorSpiPort;
import br.com.poc.pix.domain.port.out.EventPublisherPort;
import br.com.poc.pix.domain.port.out.PixRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Processa um Pix recebido: idempotencia -> EM_PROCESSAMENTO -> chamada ao SPI -> estado final.
 *
 * <p>Se o SPI estiver indisponivel (apos retries/CB), a excecao propaga e o listener envia a
 * mensagem a DLQ; o estado permanece EM_PROCESSAMENTO (constitution P3.4 - opcao a).</p>
 */
@Service
public class ProcessarPixService implements ProcessarPixUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarPixService.class);

    private final PixRepositoryPort repositorio;
    private final AutorizadorSpiPort autorizador;
    private final EventPublisherPort publicador;

    public ProcessarPixService(PixRepositoryPort repositorio,
                               AutorizadorSpiPort autorizador,
                               EventPublisherPort publicador) {
        this.repositorio = repositorio;
        this.autorizador = autorizador;
        this.publicador = publicador;
    }

    @Override
    public void processar(PixRecebido evento) {
        EndToEndId endToEndId = new EndToEndId(evento.endToEndId());

        Optional<StatusPix> atual = repositorio.statusAtual(endToEndId);
        if (atual.isEmpty()) {
            log.warn("Evento sem transacao correspondente endToEndId={} - ignorado", evento.endToEndId());
            return;
        }
        if (atual.get() != StatusPix.RECEBIDO) {
            // Idempotencia (P4): entrega duplicada de algo ja em processamento/finalizado.
            log.debug("Evento ignorado por idempotencia endToEndId={} status={}", evento.endToEndId(), atual.get());
            return;
        }

        repositorio.atualizarStatus(endToEndId, StatusPix.EM_PROCESSAMENTO);
        publicador.publicar(new PixEmProcessamento(endToEndId.valor(), Instant.now()));

        // Chamada bloqueante ao SPI (protegida por Resilience4j no adapter).
        ResultadoAutorizacao resultado = autorizador.autorizar(endToEndId, new Valor(evento.valor()));

        if (resultado.autorizado()) {
            repositorio.atualizarStatus(endToEndId, StatusPix.AUTORIZADO);
            publicador.publicar(new PixAutorizado(endToEndId.valor(), Instant.now()));
            log.info("Pix AUTORIZADO endToEndId={}", evento.endToEndId());
        } else {
            repositorio.atualizarStatus(endToEndId, StatusPix.REJEITADO, resultado.motivo());
            publicador.publicar(new PixRejeitado(endToEndId.valor(), resultado.motivo(), Instant.now()));
            log.info("Pix REJEITADO endToEndId={} motivo={}", evento.endToEndId(), resultado.motivo());
        }
    }
}
