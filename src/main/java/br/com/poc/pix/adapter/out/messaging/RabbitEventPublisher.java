package br.com.poc.pix.adapter.out.messaging;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;
import br.com.poc.pix.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter de saida: publica eventos de dominio no RabbitMQ (JSON).
 *
 * <p><b>Best-effort (constitution P6):</b> falha na publicacao e logada como debito conhecido
 * (nao ha Transactional Outbox nesta POC) e NAO propaga erro para o request de comando.</p>
 */
@Component
public class RabbitEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicar(PixRecebido evento) {
        enviar(RabbitConfig.RK_RECEBIDO, evento, evento.endToEndId(), "PixRecebido");
    }

    @Override
    public void publicar(PixEmProcessamento evento) {
        enviar(RabbitConfig.RK_EM_PROCESSAMENTO, evento, evento.endToEndId(), "PixEmProcessamento");
    }

    @Override
    public void publicar(PixAutorizado evento) {
        enviar(RabbitConfig.RK_AUTORIZADO, evento, evento.endToEndId(), "PixAutorizado");
    }

    @Override
    public void publicar(PixRejeitado evento) {
        enviar(RabbitConfig.RK_REJEITADO, evento, evento.endToEndId(), "PixRejeitado");
    }

    private void enviar(String routingKey, Object evento, String endToEndId, String tipo) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, evento);
            log.debug("Evento {} publicado endToEndId={}", tipo, endToEndId);
        } catch (AmqpException e) {
            // Best-effort (constitution P6): nao ha outbox; falha e logada como debito conhecido.
            log.warn("Falha ao publicar {} endToEndId={} (best-effort, sem outbox)", tipo, endToEndId, e);
        }
    }
}
