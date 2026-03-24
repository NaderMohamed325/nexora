package com.neo.nexora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Long    recipientId;
    private Long    actorId;
    private String  actorUsername;
    private String  type;
    private String  entityType;
    private Long    entityId;
    private String  message;
    private Instant createdAt;
}