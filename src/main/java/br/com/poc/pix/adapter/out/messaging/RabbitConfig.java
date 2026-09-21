package br.com.poc.pix.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ do Pix: exchange de eventos, fila do consumer com dead-lettering e DLQ.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "pix.exchange";
    public static final String RK_RECEBIDO = "pix.recebido";
    public static final String RK_EM_PROCESSAMENTO = "pix.em_processamento";
    public static final String RK_AUTORIZADO = "pix.autorizado";
    public static final String RK_REJEITADO = "pix.rejeitado";

    public static final String Q_RECEBIDO = "pix.recebido.q";
    public static final String Q_PROJECAO = "pix.projecao.q";
    public static final String DLX = "pix.dlx";
    public static final String Q_DLQ = "pix.recebido.dlq";

    @Bean
    public TopicExchange pixExchange() {
        // durable=true, autoDelete=false
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Fila do consumer, com dead-letter para a DLX quando a mensagem for rejeitada. */
    @Bean
    public Queue filaRecebido() {
        return QueueBuilder.durable(Q_RECEBIDO)
                .withArgument("x-dead-letter-exchange", DLX)
                .build();
    }

    @Bean
    public Binding bindingRecebido(Queue filaRecebido, TopicExchange pixExchange) {
        return BindingBuilder.bind(filaRecebido).to(pixExchange).with(RK_RECEBIDO);
    }

    /** Dead-letter exchange (fanout) e sua fila. */
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX, true, false);
    }

    @Bean
    public Queue filaDlq() {
        return QueueBuilder.durable(Q_DLQ).build();
    }

    @Bean
    public Binding bindingDlq(Queue filaDlq, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(filaDlq).to(deadLetterExchange);
    }

    // ===== Projecao (read model - fatia 004) =====
    // Fila propria que recebe uma copia dos 4 eventos do ciclo de vida.

    @Bean
    public Queue filaProjecao() {
        return QueueBuilder.durable(Q_PROJECAO).build();
    }

    @Bean
    public Binding bindingProjecaoRecebido(Queue filaProjecao, TopicExchange pixExchange) {
        return BindingBuilder.bind(filaProjecao).to(pixExchange).with(RK_RECEBIDO);
    }

    @Bean
    public Binding bindingProjecaoProcessando(Queue filaProjecao, TopicExchange pixExchange) {
        return BindingBuilder.bind(filaProjecao).to(pixExchange).with(RK_EM_PROCESSAMENTO);
    }

    @Bean
    public Binding bindingProjecaoAutorizado(Queue filaProjecao, TopicExchange pixExchange) {
        return BindingBuilder.bind(filaProjecao).to(pixExchange).with(RK_AUTORIZADO);
    }

    @Bean
    public Binding bindingProjecaoRejeitado(Queue filaProjecao, TopicExchange pixExchange) {
        return BindingBuilder.bind(filaProjecao).to(pixExchange).with(RK_REJEITADO);
    }

    /**
     * Usa o ObjectMapper auto-configurado pelo Spring Boot (com JavaTimeModule),
     * garantindo serializacao ISO-8601 do Instant. Define os pacotes confiaveis para que a
     * projecao (@RabbitHandler multi-tipo) possa desserializar por {@code __TypeId__}.
     */
    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("br.com.poc.pix.domain.event");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
