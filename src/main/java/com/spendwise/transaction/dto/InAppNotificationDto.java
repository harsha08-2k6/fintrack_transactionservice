package com.spendwise.transaction.dto;

import java.time.LocalDateTime;

public class InAppNotificationDto {
    private String id;
    private Long userId;
    private String title;
    private String message;
    private String category;
    private String severity;
    private boolean read;
    private String actionUrl;
    private String metadata;
    private LocalDateTime timestamp;

    public InAppNotificationDto() {}

    public InAppNotificationDto(String id, Long userId, String title, String message, String category, String severity, boolean read, String actionUrl, String metadata, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.category = category;
        this.severity = severity;
        this.read = read;
        this.actionUrl = actionUrl;
        this.metadata = metadata;
        this.timestamp = timestamp;
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

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
