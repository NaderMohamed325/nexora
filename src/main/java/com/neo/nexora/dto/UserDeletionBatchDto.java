package com.neo.nexora.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object that represents a single batch of user IDs queued for deletion.
 *
 * <p>This DTO is the <b>message payload</b> serialized to JSON and sent through the
 * RabbitMQ user deletion pipeline:</p>
 * <pre>
 *   Producer → JSON → Exchange → Queue → JSON → Consumer
 * </pre>
 *
 * <h3>Example JSON on the wire</h3>
 * <pre>{@code
 * {
 *   "batchIndex": 0,
 *   "totalBatches": 5,
 *   "userIds": [101, 102, 103, ..., 2100]
 * }
 * }</pre>
 *
 * <h3>Size considerations</h3>
 * <ul>
 *   <li>Each batch carries at most {@value com.neo.nexora.config.RabbitMQConfig#BATCH_SIZE} IDs.</li>
 *   <li>2000 IDs × 8 bytes ≈ 16 KB per message — lightweight for RabbitMQ.</li>
 * </ul>
 *
 * @see com.neo.nexora.config.RabbitMQConfig                             Queue / exchange / binding declarations
 * @see com.neo.nexora.service.queue.producer.UserDeletionProducerImpl   Publishes these DTOs
 * @see com.neo.nexora.service.queue.consumer.UserDeletionConsumerImpl   Consumes and processes these DTOs
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDeletionBatchDto {

    /**
     * Zero-based index of this batch within the current scheduled run.
     * <p>Used for progress logging (e.g., "Processing batch 3/10").</p>
     */
    @PositiveOrZero(message = "Batch index must be zero or positive")
    private int batchIndex;

    /**
     * Total number of batches produced in the current scheduled run.
     * <p>Used alongside {@link #batchIndex} for progress logging.</p>
     */
    @PositiveOrZero(message = "Total batches must be zero or positive")
    private int totalBatches;

    /**
     * The actual payload — a list of user IDs to be permanently deleted.
     * <p>Contains at most {@value com.neo.nexora.config.RabbitMQConfig#BATCH_SIZE} entries.
     * The consumer will further partition these into sub-batches of 500
     * before executing {@code DELETE} statements.</p>
     */
    @NotNull(message = "User IDs list cannot be null")
    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;
}
