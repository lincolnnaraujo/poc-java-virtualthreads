package br.com.poc.pix.adapter.in.messaging;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Fabrica do container de listeners com <b>executor plugavel</b> (o eixo do benchmark - fatia 007).
 *
 * <ul>
 *   <li>{@code benchmark.consumer.mode=platform} (default): pool fixo de plataforma;</li>
 *   <li>{@code benchmark.consumer.mode=virtual}: {@code newVirtualThreadPerTaskExecutor}.</li>
 * </ul>
 *
 * <p>{@code defaultRequeueRejected=false} garante que rejeicoes vao para a DLQ (via DLX),
 * sem loop de requeue.</p>
 */
@Configuration
public class RabbitListenerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter,
            @Value("${benchmark.consumer.mode:platform}") String mode,
            @Value("${benchmark.consumer.concurrency:20}") int concurrency) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(concurrency);
        factory.setPrefetchCount(1);

        if ("virtual".equalsIgnoreCase(mode)) {
            // Virtual threads: baratas mesmo com concorrencia alta; um executor unbounded
            // e compartilhado com seguranca entre os containers de listener.
            factory.setTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
        }
        // Modo platform (default): sem taskExecutor customizado. Cada container usa seu proprio
        // SimpleAsyncTaskExecutor e sobe exatamente `concurrency` threads de PLATAFORMA. O "limite"
        // de plataforma vem do numero de consumers, que a fatia 007 mantem menor que no modo virtual.

        return factory;
    }
}
