package com.spendwise.transaction.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "in_app_notifications", indexes = {
        @Index(name = "idx_inapp_user_date", columnList = "user_id, created_at"),
        @Index(name = "idx_inapp_user_read", columnList = "user_id, is_read")
})
public class InAppNotification {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String category = "TRANSACTION"; // BUDGET, TRANSACTION, GOAL, SUMMARY, SECURITY, INSIGHT

    @Column(nullable = false, length = 30)
    private String severity = "INFO"; // INFO, SUCCESS, WARNING, CRITICAL

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "action_url", length = 100)
    private String actionUrl;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public InAppNotification() {}

    public InAppNotification(String id, Long userId, String title, String message, String category, String severity, String actionUrl, String metadataJson) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.category = category != null ? category : "TRANSACTION";
        this.severity = severity != null ? severity : "INFO";
        this.actionUrl = actionUrl;
        this.metadataJson = metadataJson;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
