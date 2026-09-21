package br.com.poc.pix.adapter.in.messaging;

import br.com.poc.pix.adapter.out.messaging.RabbitConfig;
import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;
import br.com.poc.pix.domain.port.in.ProjetarPixUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila de projecao: recebe os eventos de dominio (roteados por {@code __TypeId__})
 * e atualiza o read model. O despacho por tipo usa {@link RabbitHandler}.
 */
@Component
@RabbitListener(queues = RabbitConfig.Q_PROJECAO)
public class ProjecaoPixListener {

    private final ProjetarPixUseCase projetar;

    public ProjecaoPixListener(ProjetarPixUseCase projetar) {
        this.projetar = projetar;
    }

    @RabbitHandler
    public void aoReceber(PixRecebido evento) {
        projetar.aoReceber(evento);
    }

    @RabbitHandler
    public void aoProcessar(PixEmProcessamento evento) {
        projetar.aoProcessar(evento);
    }

    @RabbitHandler
    public void aoAutorizar(PixAutorizado evento) {
        projetar.aoAutorizar(evento);
    }

    @RabbitHandler
    public void aoRejeitar(PixRejeitado evento) {
        projetar.aoRejeitar(evento);
    }
}
