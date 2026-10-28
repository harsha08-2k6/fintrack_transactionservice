package com.spendwise.transaction.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_deliveries", indexes = {
        @Index(name = "idx_deliv_user", columnList = "user_id"),
        @Index(name = "idx_deliv_status", columnList = "delivery_status"),
        @Index(name = "idx_deliv_idemp", columnList = "idempotency_key", unique = true)
})
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(name = "delivery_status", nullable = false, length = 30)
    private String deliveryStatus = "PENDING"; // PENDING, SENT, FAILED, DISABLED

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "error_category", length = 50)
    private String errorCategory; // NONE, AUTH_FAILURE, TIMEOUT, CONNECTION_ERROR, INVALID_RECIPIENT, DISABLED, UNKNOWN

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "idempotency_key", length = 150, unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    public NotificationDelivery() {}

    public NotificationDelivery(Long userId, String eventId, String notificationType, String recipientEmail, String idempotencyKey) {
        this.userId = userId;
        this.eventId = eventId;
        this.notificationType = notificationType;
        this.recipientEmail = recipientEmail;
        this.idempotencyKey = idempotencyKey;
        this.deliveryStatus = "PENDING";
        this.attemptCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public String getErrorCategory() { return errorCategory; }
    public void setErrorCategory(String errorCategory) { this.errorCategory = errorCategory; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
}
