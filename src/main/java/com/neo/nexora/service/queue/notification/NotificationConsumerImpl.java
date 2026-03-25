package com.neo.nexora.service.queue.notification;

import com.neo.nexora.config.RabbitMQConfig;
import com.neo.nexora.dto.NotificationMessage;
import com.neo.nexora.entity.Notification;
import com.neo.nexora.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationConsumerImpl implements NotificationConsumer {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consume(NotificationMessage message) {
        try {
            Notification entity = Notification.builder()
                    .recipientId(message.getRecipientId())
                    .actorId(message.getActorId())
                    .type(message.getType())
                    .entityType(message.getEntityType())
                    .entityId(message.getEntityId())
                    .message(message.getMessage())
                    .read(false)
                    .build();

            notificationRepository.save(entity);

            // Send real-time notification via WebSocket
            String destination = "/topic/notifications/" + message.getRecipientId();
            messagingTemplate.convertAndSend(destination, message);

            log.info("Notification sent to user {} via WebSocket at {}",
                    message.getRecipientId(), destination);

        } catch (Exception e) {
            log.error("Failed to process notification for recipient={}", message.getRecipientId(), e);
            throw e;
        }
    }
}
