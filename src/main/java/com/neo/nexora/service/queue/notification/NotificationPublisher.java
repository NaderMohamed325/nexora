package com.neo.nexora.service.queue.notification;

import com.neo.nexora.dto.NotificationMessage;

public interface NotificationPublisher {
    public void publishNotification(NotificationMessage message);

    public void notifyLike(Long recipientId, Long actorId, String actorUsername, String entityType, Long entityId);

    public void notifyFollow(Long recipientId, Long actorId, String actorUsername);

    public void notifyComment(Long recipientId, Long actorId, String actorUsername, String entityType, Long entityId);
}
