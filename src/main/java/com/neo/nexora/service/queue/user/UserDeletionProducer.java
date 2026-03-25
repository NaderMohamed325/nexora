package com.neo.nexora.service.queue.user;

/**
 * Contract for the user deletion message producer.
 *
 * <p>Implementations are responsible for querying users that are scheduled for deletion
 * and publishing their IDs as batched messages to the RabbitMQ user deletion queue.</p>
 *
 * <h3>Typical flow</h3>
 * <ol>
 *   <li>A scheduler triggers {@link #scheduleUserDeletion()} (e.g., daily at 2 AM).</li>
 *   <li>Users whose {@code scheduledForDeletionAt} timestamp has passed are fetched page-by-page.</li>
 *   <li>Each page of user IDs is wrapped in a
 *       {@link com.neo.nexora.dto.UserDeletionBatchDto} and sent to the exchange.</li>
 * </ol>
 *
 * @see UserDeletionProducerImpl
 * @see com.neo.nexora.config.RabbitMQConfig
 */
public interface UserDeletionProducer {

    /**
     * Queries the database for users scheduled for deletion and publishes
     * batched messages to the RabbitMQ user deletion queue.
     *
     * <p>This method is expected to be triggered by a scheduler (e.g., {@code @Scheduled})
     * and should be idempotent — running it multiple times in the same day may produce
     * duplicate messages, but the consumer handles deletion safely via
     * {@code DELETE WHERE id IN (...)}.</p>
     */
    void scheduleUserDeletion();
}
