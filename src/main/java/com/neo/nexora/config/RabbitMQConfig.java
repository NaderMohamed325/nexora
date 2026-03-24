package com.neo.nexora.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.neo.nexora.service.queue.user.UserDeletionConsumerImpl;
import com.neo.nexora.service.queue.user.UserDeletionProducerImpl;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /**
     * Queue name where user deletion batch messages are stored until consumed.
     */
    public static final String QUEUE_NAME = "user.deletion.queue";

    /**
     * Direct exchange name that routes messages to the deletion queue by routing key.
     */
    public static final String EXCHANGE_NAME = "user.deletion.exchange";

    /**
     * Routing key that binds the exchange to the queue — must match on both producer and binding.
     */
    public static final String ROUTING_KEY = "user.deletion.key";

    /**
     * Maximum number of user IDs per message/batch.
     * <p>
     * Chosen to balance memory usage vs. number of messages:
     * <ul>
     *   <li>2000 IDs × 8 bytes = ~16 KB per message (lightweight)</li>
     *   <li>100,000 users ÷ 2000 = 50 messages (manageable queue depth)</li>
     * </ul>
     */
    public static final int BATCH_SIZE = 2000;

    // Add these alongside your existing constants
    public static final String NOTIFICATION_QUEUE       = "notifications.queue";
    public static final String NOTIFICATION_EXCHANGE    = "notifications.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notifications.#";
    public static final String NOTIFICATION_DLX         = "notifications.dlx";
    public static final String NOTIFICATION_DLQ         = "notifications.dlq";
    /**
     * Declares the durable user deletion queue.
     * <p>
     * <b>Durable</b> — the queue definition and its messages survive RabbitMQ restarts.<br>
     * <b>x-message-ttl = 86,400,000 ms (24 hours)</b> — if a message sits unconsumed for 24 hours
     * (e.g., consumer is down), it is automatically discarded to prevent unbounded queue growth.
     *
     * @return a durable {@link Queue} named {@value #QUEUE_NAME}
     */
    @Bean
    public Queue userDeletionQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    /**
     * Declares the Direct Exchange for user deletion routing.
     * <p>
     * A <b>Direct Exchange</b> routes messages to queues whose binding key exactly matches
     * the message's routing key. This is the simplest and most explicit routing strategy.
     *
     * @return a {@link DirectExchange} named {@value #EXCHANGE_NAME}
     */
    @Bean
    public DirectExchange userDeletionExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    /**
     * Binds the user deletion queue to the exchange using the routing key.
     * <p>
     * This tells RabbitMQ: <i>"Any message sent to {@value #EXCHANGE_NAME} with routing key
     * {@value #ROUTING_KEY} should be delivered to {@value #QUEUE_NAME}."</i>
     *
     * @param userDeletionQueue    the queue bean to bind
     * @param userDeletionExchange the exchange bean to bind to
     * @return the {@link Binding} linking exchange → queue via routing key
     */
    @Bean
    public Binding binding(Queue userDeletionQueue, DirectExchange userDeletionExchange) {
        return BindingBuilder
                .bind(userDeletionQueue)
                .to(userDeletionExchange)
                .with(ROUTING_KEY);
    }

    /**
     * Configures the Jackson {@link ObjectMapper} used for JSON serialization/deserialization
     * of RabbitMQ message payloads.
     * <p>
     * {@code findAndAddModules()} auto-discovers modules like {@code JavaTimeModule}
     * so that {@code LocalDateTime}, {@code Instant}, etc. are serialized correctly.
     *
     * @return a configured {@link ObjectMapper}
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    /**
     * Creates the JSON message converter used by both the {@link RabbitTemplate} (producer side)
     * and the {@link SimpleRabbitListenerContainerFactory} (consumer side).
     * <p>
     * All messages on the queue are serialized as JSON, making them:
     * <ul>
     *   <li>Human-readable in the RabbitMQ Management UI</li>
     *   <li>Language-agnostic (other services could consume them)</li>
     *   <li>Debuggable via standard JSON tools</li>
     * </ul>
     *
     * <p><b>Note:</b> {@code Jackson2JsonMessageConverter} is deprecated since Spring AMQP 4.0
     * and marked for removal. {@code @SuppressWarnings("removal")} is applied at class level
     * until the replacement ({@code JacksonMessageConverter}) is available in a stable release.</p>
     *
     * @param objectMapper the Jackson ObjectMapper to use for JSON processing
     * @return a {@link MessageConverter} that serializes/deserializes messages as JSON
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    /**
     * Configures the {@link RabbitTemplate} used by the producer to send messages.
     * <p>
     * The template is wired with:
     * <ul>
     *   <li>{@link ConnectionFactory} — manages connections to the RabbitMQ broker</li>
     *   <li>{@link MessageConverter} — serializes {@link com.neo.nexora.dto.UserDeletionBatchDto}
     *       to JSON before sending</li>
     * </ul>
     *
     * @param connectionFactory Spring-managed RabbitMQ connection factory (auto-configured)
     * @param messageConverter  the JSON message converter bean
     * @return a configured {@link RabbitTemplate}
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * Configures the listener container factory for {@code @RabbitListener} consumers.
     * <p>
     * <b>Concurrency settings:</b>
     * <ul>
     *   <li>{@code concurrentConsumers = 3} — 3 threads process messages in parallel at startup</li>
     *   <li>{@code maxConcurrentConsumers = 5} — scales up to 5 threads under high load</li>
     * </ul>
     * This means up to 5 batches (5 × 2000 = 10,000 users) can be processed concurrently.
     *
     * @param connectionFactory Spring-managed RabbitMQ connection factory
     * @param messageConverter  the JSON message converter for deserializing incoming messages
     * @return a configured {@link SimpleRabbitListenerContainerFactory}
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}