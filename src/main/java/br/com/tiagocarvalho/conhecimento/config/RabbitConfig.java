package br.com.tiagocarvalho.conhecimento.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do RabbitMQ:
 *
 * documentos (exchange) --processar--> documentos.processar (fila)
 *                                          | mensagem rejeitada
 *                                          v
 * documentos.dlx (exchange) --processar--> documentos.processar.dlq (fila de mensagens mortas)
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "documentos";
    public static final String ROTA = "processar";
    public static final String FILA = "documentos.processar";
    public static final String EXCHANGE_MORTAS = "documentos.dlx";
    public static final String FILA_MORTAS = "documentos.processar.dlq";

    @Bean
    public DirectExchange exchangeDocumentos() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange exchangeMortas() {
        return new DirectExchange(EXCHANGE_MORTAS);
    }

    @Bean
    public Queue filaProcessamento() {
        return QueueBuilder.durable(FILA)
                .deadLetterExchange(EXCHANGE_MORTAS)
                .deadLetterRoutingKey(ROTA)
                .build();
    }

    @Bean
    public Queue filaMortas() {
        return QueueBuilder.durable(FILA_MORTAS).build();
    }

    @Bean
    public Binding ligacaoProcessamento() {
        return BindingBuilder.bind(filaProcessamento()).to(exchangeDocumentos()).with(ROTA);
    }

    @Bean
    public Binding ligacaoMortas() {
        return BindingBuilder.bind(filaMortas()).to(exchangeMortas()).with(ROTA);
    }

    /** Mensagens trafegam em JSON (o Spring Boot aplica este conversor ao RabbitTemplate e aos listeners). */
    @Bean
    public MessageConverter conversorJson() {
        return new Jackson2JsonMessageConverter();
    }
}
