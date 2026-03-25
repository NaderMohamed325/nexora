package com.neo.nexora.service.queue.notification;

import com.neo.nexora.config.RabbitMQConfig;
import com.neo.nexora.dto.NotificationMessage;
import com.neo.nexora.entity.Notification;
import com.neo.nexora.entity.User;
import com.neo.nexora.repository.NotificationRepository;
import com.neo.nexora.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    @Override
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consume(NotificationMessage message) {
        try {
            User recipient = userRepository.findById(message.getRecipientId())
                    .orElseThrow(() -> new RuntimeException("Recipient user not found: " + message.getRecipientId()));

            User actor = userRepository.findById(message.getActorId())
                    .orElseThrow(() -> new RuntimeException("Actor user not found: " + message.getActorId()));
            Notification entity = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(message.getType())
                    .entityType(message.getEntityType())
                    .entityId(message.getEntityId())
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
