package com.neo.nexora.service.queue.notification;

import com.neo.nexora.dto.NotificationMessage;

public interface NotificationConsumer {
    public void consume(NotificationMessage message);
}
