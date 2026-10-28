package com.spendwise.transaction.dto;

public class EmailSendResult {
    private final boolean success;
    private final String status; // SENT, FAILED, DISABLED
    private final String errorCategory; // NONE, AUTH_FAILURE, TIMEOUT, CONNECTION_ERROR, INVALID_RECIPIENT, DISABLED, UNKNOWN
    private final String errorMessage;
    private final int attempts;

    public EmailSendResult(boolean success, String status, String errorCategory, String errorMessage, int attempts) {
        this.success = success;
        this.status = status;
        this.errorCategory = errorCategory;
        this.errorMessage = errorMessage;
        this.attempts = attempts;
    }

    public static EmailSendResult sent(int attempts) {
        return new EmailSendResult(true, "SENT", "NONE", null, attempts);
    }

    public static EmailSendResult disabled() {
        return new EmailSendResult(false, "DISABLED", "DISABLED", "SMTP notifications are currently disabled in configuration", 0);
    }

    public static EmailSendResult failed(String category, String message, int attempts) {
        return new EmailSendResult(false, "FAILED", category, message, attempts);
    }

    public boolean isSuccess() { return success; }
    public String getStatus() { return status; }
    public String getErrorCategory() { return errorCategory; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttempts() { return attempts; }
}
