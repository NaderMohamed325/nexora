package com.neo.nexora.dto;

import lombok.*;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Long recipientId;
    private Long actorId;
    private String actorUsername;
    private String type;
    private String entityType;
    private Long entityId;
    private String message;
    private Instant createdAt;
}