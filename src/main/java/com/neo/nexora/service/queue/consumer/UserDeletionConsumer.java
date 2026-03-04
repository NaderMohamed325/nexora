package com.neo.nexora.service.queue.consumer;

import com.neo.nexora.dto.UserDeletionBatchDto;

/**
 * Contract for the user deletion message consumer.
 *
 * <p>Implementations listen on the RabbitMQ user deletion queue and process
 * incoming {@link UserDeletionBatchDto} messages by deleting the referenced
 * users from the database.</p>
 *
 * <h3>Consumer responsibilities</h3>
 * <ul>
 *   <li>Deserialize the incoming JSON message into a {@link UserDeletionBatchDto}.</li>
 *   <li>Delete the users identified by the batch's user IDs.</li>
 *   <li>Re-throw exceptions so that failed messages are re-queued and retried.</li>
 * </ul>
 *
 * @see UserDeletionConsumerImpl
 * @see com.neo.nexora.config.RabbitMQConfig
 */
public interface UserDeletionConsumer {

    /**
     * Processes a single batch of user deletions received from the queue.
     *
     * <p>On success the message is acknowledged and removed from the queue.
     * On failure the implementation <b>must</b> propagate the exception so that
     * Spring AMQP can re-queue the message for retry.</p>
     *
     * @param batch the deserialized message containing the list of user IDs to delete,
     *              along with batch-index and total-batches metadata for progress logging
     */
    void processDeletionBatch(UserDeletionBatchDto batch);
}
