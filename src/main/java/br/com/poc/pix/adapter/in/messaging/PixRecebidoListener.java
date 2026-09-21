package br.com.poc.pix.adapter.in.messaging;

import br.com.poc.pix.adapter.out.messaging.RabbitConfig;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.port.in.ProcessarPixUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada dirigido por mensageria: consome {@link PixRecebido} e dispara o processamento.
 *
 * <p>Em caso de falha (SPI indisponivel apos retries, dado invalido, etc.) incrementa
 * {@code pix_dlq_total} e forca o envio a DLQ via {@link AmqpRejectAndDontRequeueException}
 * (sem reprocessamento automatico - constitution P3.4 opcao a).</p>
 */
@Component
public class PixRecebidoListener {

    private static final Logger log = LoggerFactory.getLogger(PixRecebidoListener.class);

    private final ProcessarPixUseCase processarPix;
    private final Counter dlqCounter;

    public PixRecebidoListener(ProcessarPixUseCase processarPix, MeterRegistry registry) {
        this.processarPix = processarPix;
        this.dlqCounter = Counter.builder("pix_dlq_total")
                .description("Total de mensagens enviadas para a DLQ")
                .register(registry);
    }

    @RabbitListener(queues = RabbitConfig.Q_RECEBIDO)
    public void onPixRecebido(PixRecebido evento) {
        try {
            processarPix.processar(evento);
        } catch (Exception e) {
            dlqCounter.increment();
            log.warn("Enviando para DLQ endToEndId={} motivo={}", evento.endToEndId(), e.toString());
            throw new AmqpRejectAndDontRequeueException("Falha no processamento; enviada a DLQ", e);
        }
    }
}
