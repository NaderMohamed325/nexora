package com.neo.nexora.service.queue.consumer;

import com.neo.nexora.config.RabbitMQConfig;
import com.neo.nexora.dto.UserDeletionBatchDto;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ consumer that listens on the user deletion queue and permanently
 * removes users from the database.
 *
 * <h2>Concurrency</h2>
 * <p>The listener container factory (configured in
 * {@link com.neo.nexora.config.RabbitMQConfig#rabbitListenerContainerFactory})
 * spins up <b>3–5 concurrent threads</b>, so multiple batches are processed in parallel.
 * Each thread independently picks one message from the queue and processes it.</p>
 *
 * <h2>Sub-batching strategy</h2>
 * <p>Each incoming message can contain up to
 * {@value com.neo.nexora.config.RabbitMQConfig#BATCH_SIZE} user IDs.
 * Issuing a single {@code DELETE WHERE id IN (2000 ids)} would lock the table
 * for too long and block other transactions. Instead, the IDs are split into
 * <b>sub-batches of 500</b> and deleted in separate transactions:</p>
 * <pre>
 *   Incoming batch: 2000 user IDs
 *      ├── sub-batch 1: IDs 1–500     → DELETE WHERE id IN (...)
 *      ├── sub-batch 2: IDs 501–1000  → DELETE WHERE id IN (...)
 *      ├── sub-batch 3: IDs 1001–1500 → DELETE WHERE id IN (...)
 *      └── sub-batch 4: IDs 1501–2000 → DELETE WHERE id IN (...)
 * </pre>
 *
 * <h2>Error handling</h2>
 * <p>If any sub-batch fails, the exception is <b>re-thrown</b>. This is critical:
 * if swallowed, Spring considers the message successfully processed, acknowledges it,
 * and the remaining users are <i>silently never deleted</i>. Re-throwing causes the
 * message to be re-queued and retried (up to the max-attempts configured in
 * {@code application.yml}).</p>
 *
 * @see com.neo.nexora.config.RabbitMQConfig                             Queue / exchange / binding declarations
 * @see com.neo.nexora.service.queue.producer.UserDeletionProducerImpl   Producer that publishes these batches
 * @see com.neo.nexora.dto.UserDeletionBatchDto                          DTO carried by each message
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeletionConsumerImpl implements UserDeletionConsumer {

    /**
     * Repository used to execute bulk {@code DELETE} operations on the {@code users} table.
     */
    private final UserRepository userRepository;


    /**
     * Processes a single user deletion batch received from the
     * {@value com.neo.nexora.config.RabbitMQConfig#QUEUE_NAME} queue.
     *
     * <p><b>Steps:</b></p>
     * <ol>
     *   <li>Log batch metadata (index, total, size).</li>
     *   <li>Partition the user IDs into sub-batches of 500.</li>
     *   <li>Delete each sub-batch via {@link com.neo.nexora.repository.UserRepository#deleteBatch(java.util.List)}.</li>
     *   <li>On failure, re-throw so the message is re-queued for retry.</li>
     * </ol>
     *
     * @param batch the deserialized {@link com.neo.nexora.dto.UserDeletionBatchDto} message
     *              containing user IDs, batch index, and total batch count
     */
    @Override
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processDeletionBatch(UserDeletionBatchDto batch) {
        log.info("Processing batch {}/{} — {} users",
                batch.getBatchIndex() + 1,
                batch.getTotalBatches(),
                batch.getUserIds().size()
        );
        try {

            List<List<Long>> subBatches = partition(batch.getUserIds(), 500);

            int subBatchIndex = 0;
            for (List<Long> subBatch : subBatches) {
                userRepository.deleteBatch(subBatch);
                log.info("Batch {}/{} — sub-batch {}/{} deleted ({} users)",
                        batch.getBatchIndex() + 1,
                        batch.getTotalBatches(),
                        ++subBatchIndex,
                        subBatches.size(),
                        subBatch.size()
                );
            }

            log.info("Batch {}/{} completed successfully.",
                    batch.getBatchIndex() + 1,
                    batch.getTotalBatches()
            );

        } catch (Exception e) {
            log.error("Failed to process batch {}/{}: {}",
                    batch.getBatchIndex() + 1,
                    batch.getTotalBatches(),
                    e.getMessage()
            );


            throw e;
        }
    }

    /**
     * Splits a list into consecutive sublists of the given maximum size.
     *
     * <p>Example: {@code partition([1,2,3,4,5,6,7], 3)} → {@code [[1,2,3], [4,5,6], [7]]}</p>
     *
     * <p>This is used to break a large batch of user IDs into smaller sub-batches
     * so that each {@code DELETE WHERE id IN (...)} statement operates on a manageable
     * number of rows (~50 ms per sub-batch), minimizing table lock duration.</p>
     *
     * @param list the source list to partition
     * @param size the maximum number of elements per sub-list
     * @param <T>  the element type
     * @return a list of consecutive sublists, each with at most {@code size} elements
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(
                    list.subList(i, Math.min(i + size, list.size()))
            );
        }
        return partitions;
    }

}

