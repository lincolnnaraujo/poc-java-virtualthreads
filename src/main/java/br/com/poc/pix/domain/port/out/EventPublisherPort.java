package br.com.poc.pix.domain.port.out;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;

/**
 * Porta de saida de publicacao de eventos de dominio.
 * A tecnologia (RabbitMQ hoje, SQS no futuro) fica atras desta porta - constitution decisao registrada.
 */
public interface EventPublisherPort {
    void publicar(PixRecebido evento);

    void publicar(PixEmProcessamento evento);

    void publicar(PixAutorizado evento);

    void publicar(PixRejeitado evento);
}
