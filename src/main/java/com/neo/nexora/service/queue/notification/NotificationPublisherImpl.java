package com.neo.nexora.service.queue.notification;

import com.neo.nexora.config.RabbitMQConfig;
import com.neo.nexora.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationPublisherImpl implements NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishNotification(NotificationMessage message) {
        String routingKey = "notifications." + message.getType().toLowerCase();
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, routingKey, message);
    }

    @Override
    public void notifyLike(Long recipientId, Long actorId, String actorUsername, String entityType, Long entityId) {
        NotificationMessage message = new NotificationMessage();
        message.setRecipientId(recipientId);
        message.setActorId(actorId);
        message.setActorUsername(actorUsername);
        message.setType("LIKE");
        message.setEntityType(entityType);
        message.setEntityId(entityId);
        message.setMessage(String.format("%s liked your %s.", actorUsername, entityType.toLowerCase()));
        message.setCreatedAt(Instant.now());
        publishNotification(message);
    }

    @Override
    public void notifyFollow(Long recipientId, Long actorId, String actorUsername) {
        NotificationMessage message = new NotificationMessage();
        message.setRecipientId(recipientId);
        message.setActorId(actorId);
        message.setActorUsername(actorUsername);
        message.setType("FOLLOW");
        message.setMessage(String.format("%s started following you.", actorUsername));
        message.setCreatedAt(Instant.now());
        publishNotification(message);
    }

    @Override
    public void notifyComment(Long recipientId, Long actorId, String actorUsername, String entityType, Long entityId) {
        NotificationMessage message = new NotificationMessage();
        message.setRecipientId(recipientId);
        message.setActorId(actorId);
        message.setActorUsername(actorUsername);
        message.setType("COMMENT");
        message.setEntityType(entityType);
        message.setEntityId(entityId);
        message.setMessage(String.format("%s commented on your %s.", actorUsername, entityType.toLowerCase()));
        message.setCreatedAt(Instant.now());
        publishNotification(message);
    }
}
