package com.neo.nexora.service.queue.producer;

import com.neo.nexora.config.RabbitMQConfig;
import com.neo.nexora.dto.UserDeletionBatchDto;
import com.neo.nexora.entity.User;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled producer that publishes user deletion batches to RabbitMQ.
 *
 * <h2>Responsibility</h2>
 * <p>Every day at <b>2:00 AM</b>, this service queries the database for users whose
 * {@code scheduledForDeletionAt} timestamp has passed, groups their IDs into pages of
 * {@value com.neo.nexora.config.RabbitMQConfig#BATCH_SIZE}, wraps each page in a
 * {@link UserDeletionBatchDto}, and publishes it to the
 * {@value com.neo.nexora.config.RabbitMQConfig#EXCHANGE_NAME} exchange
 * with routing key {@value com.neo.nexora.config.RabbitMQConfig#ROUTING_KEY}.</p>
 *
 * <h2>Pagination strategy</h2>
 * <pre>
 *   page 0 → LIMIT 2000 OFFSET 0     → users 1–2000
 *   page 1 → LIMIT 2000 OFFSET 2000  → users 2001–4000
 *   page 2 → LIMIT 2000 OFFSET 4000  → users 4001–6000
 *   ...
 * </pre>
 * <p>Only user IDs (8 bytes each) are sent — not full User entities — to keep
 * message payloads lightweight (~16 KB per batch of 2000).</p>
 *
 * <h2>Message lifecycle</h2>
 * <pre>
 *   scheduleUserDeletion()
 *      │
 *      ▼
 *   RabbitTemplate.convertAndSend(exchange, routingKey, batch)
 *      │  → Jackson serializes UserDeletionBatchDto to JSON
 *      │  → message sent to "user.deletion.exchange"
 *      ▼
 *   Exchange routes via "user.deletion.key" → "user.deletion.queue"
 *      │
 *      ▼
 *   Message waits in durable queue until consumer picks it up
 * </pre>
 *
 * @see com.neo.nexora.config.RabbitMQConfig         RabbitMQ infrastructure (queue, exchange, binding)
 * @see com.neo.nexora.service.queue.consumer.UserDeletionConsumerImpl  Consumer that processes these batches
 * @see UserDeletionBatchDto                         DTO carried by each message
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeletionProducerImpl implements UserDeletionProducer {

    /** Template used to convert and send {@link UserDeletionBatchDto} messages to RabbitMQ. */
    private final RabbitTemplate rabbitTemplate;

    /** Repository used to query users whose {@code scheduledForDeletionAt} has passed. */
    private final UserRepository userRepository;

    /**
     * Scheduled job — runs every day at 2:00 AM.
     *
     * <p><b>Cron expression:</b> {@code 0 0 2 * * *}</p>
     * <table>
     *   <tr><th>Field</th><th>Value</th><th>Meaning</th></tr>
     *   <tr><td>Second</td><td>0</td><td>at second :00</td></tr>
     *   <tr><td>Minute</td><td>0</td><td>at minute :00</td></tr>
     *   <tr><td>Hour</td><td>2</td><td>at 2 AM</td></tr>
     *   <tr><td>Day</td><td>*</td><td>every day</td></tr>
     *   <tr><td>Month</td><td>*</td><td>every month</td></tr>
     *   <tr><td>Weekday</td><td>*</td><td>every weekday</td></tr>
     * </table>
     *
     * <p>The method iterates page-by-page through eligible users, publishing one
     * {@link UserDeletionBatchDto} per page to the RabbitMQ exchange. It stops when
     * there are no more pages.</p>
     */
    @Override
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleUserDeletion() {
        log.info("========== User deletion job started ==========");

        int page = 0;
        Page<User> userPage;

        do {

            userPage = userRepository.findUsersScheduledForDeletion(LocalDateTime.now(),
                    PageRequest.of(page, RabbitMQConfig.BATCH_SIZE)
            );


            List<Long> userIds = userPage.getContent()
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            if (!userIds.isEmpty()) {

                UserDeletionBatchDto batch = new UserDeletionBatchDto(
                        page,                        // batchIndex
                        userPage.getTotalPages(),    // totalBatches
                        userIds                      // payload
                );

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.ROUTING_KEY,
                        batch
                );

                log.info("Published batch {}/{} — {} users queued for deletion",
                        page + 1,
                        userPage.getTotalPages(),
                        userIds.size()
                );
            }

            page++;

        } while (userPage.hasNext());

        log.info("========== All batches published. Total pages: {} ==========", page);
    }
}