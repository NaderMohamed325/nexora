package com.neo.nexora.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notifications_unread",    columnList = "recipient_id, read")
})
public class Notification extends AuditData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false)
    private String type;

    private String entityType;

    private Long entityId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean read;


}