package com.spendwise.transaction.service.impl;

import com.spendwise.transaction.dto.EmailContent;
import com.spendwise.transaction.dto.EmailSendResult;
import com.spendwise.transaction.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${fintrack.mail.enabled:false}")
    private boolean smtpEnabled;

    @Value("${fintrack.mail.from-email:no-reply@fintrack.com}")
    private String fromEmail;

    @Value("${fintrack.mail.from-name:FinTrack Notifications}")
    private String fromName;

    @Value("${GOOGLE_SCRIPT_MAIL_URL:${fintrack.mail.google-script-url:}}")
    private String googleScriptUrl;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        if (googleScriptUrl != null && !googleScriptUrl.isBlank()) {
            String masked = googleScriptUrl.length() > 35 
                    ? googleScriptUrl.substring(0, 35) + "..." 
                    : googleScriptUrl;
            log.info("📧 Transaction Service: Google Apps Script Email Relay is ACTIVE. Target: {}", masked);
        } else {
            log.info("📧 Transaction Service: Google Apps Script Email Relay is NOT set");
        }
    }

    @Override
    public boolean isSmtpEnabled() {
        return smtpEnabled || (googleScriptUrl != null && !googleScriptUrl.isBlank());
    }

    @Override
    public EmailSendResult sendEmail(String toEmail, String recipientName, EmailContent content) {
        if (content == null) {
            return EmailSendResult.failed("INVALID_CONTENT", "Email content cannot be null", 0);
        }
        return sendEmail(toEmail, recipientName, content.getSubject(), content.getHtmlBody(), content.getTextBody());
    }

    @Override
    public EmailSendResult sendEmail(String toEmail, String recipientName, String subject, String htmlBody, String textBody) {
        if (toEmail == null || toEmail.trim().isEmpty() || !toEmail.contains("@")) {
            log.warn("Email send rejected: missing or invalid recipient address: {}", toEmail);
            return EmailSendResult.failed("INVALID_RECIPIENT", "Recipient email address is missing or invalid", 0);
        }

        if (!isSmtpEnabled()) {
            log.info("Email dispatch skipped because SMTP and HTTPS relays are disabled. Intended recipient: {}", maskEmail(toEmail));
            return EmailSendResult.disabled();
        }

        // 1. Try Google Apps Script Webhook first (Port 443 - uses Gmail without Render port 587 block)
        if (sendViaGoogleScript(toEmail.trim(), subject, htmlBody, textBody)) {
            return EmailSendResult.sent(1);
        }

        // 2. Fallback to direct SMTP if Google Script is unavailable or not configured
        int maxAttempts = 3;
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String senderAddr = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail.trim() : "no-reply@fintrack.com";
                String senderDisplay = (fromName != null && !fromName.isBlank()) ? fromName.trim() : "FinTrack Notifications";
                helper.setFrom(new InternetAddress(senderAddr, senderDisplay));

                String cleanTo = toEmail.trim();
                String cleanName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : cleanTo;
                helper.setTo(new InternetAddress(cleanTo, cleanName));

                helper.setSubject(subject != null ? subject : "FinTrack Notification");

                String plainText = (textBody != null && !textBody.isBlank()) ? textBody : "Please view this notification in an HTML-compatible client.";
                String htmlText = (htmlBody != null && !htmlBody.isBlank()) ? htmlBody : plainText;
                helper.setText(plainText, htmlText);

                mailSender.send(mimeMessage);
                log.info("Email successfully dispatched via Gmail SMTP to {} (attempt {}/{}). Subject: {}",
                        maskEmail(toEmail), attempt, maxAttempts, subject);
                return EmailSendResult.sent(attempt);

            } catch (Exception ex) {
                String category = categorizeError(ex);
                String safeMessage = sanitizeMessage(ex.getMessage());
                log.warn("SMTP delivery attempt {}/{} failed for recipient {}: category={}, reason={}",
                        attempt, maxAttempts, maskEmail(toEmail), category, safeMessage);

                // Non-retryable errors
                if ("AUTH_FAILURE".equals(category) || "INVALID_RECIPIENT".equals(category) || attempt >= maxAttempts) {
                    return EmailSendResult.failed(category, safeMessage, attempt);
                }

                // Transient failure retry delay
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return EmailSendResult.failed("INTERRUPTED", "Retry interrupted", attempt);
                }
            }
        }

        return EmailSendResult.failed("TIMEOUT", "Max retry attempts exhausted without successful SMTP submission", maxAttempts);
    }

    private boolean sendViaGoogleScript(String to, String subject, String htmlBody, String textBody) {
        if (googleScriptUrl == null || googleScriptUrl.isBlank()) {
            return false;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            String safeSubject = (subject != null) ? subject : "FinTrack Notification";
            String safeHtml = (htmlBody != null && !htmlBody.isBlank()) ? htmlBody : textBody;
            String safeText = (textBody != null && !textBody.isBlank()) ? textBody : safeSubject;

            String jsonPayload = "{\"to\":\"" + to + "\"," +
                    "\"subject\":" + quoteJson(safeSubject) + "," +
                    "\"html\":" + quoteJson(safeHtml) + "," +
                    "\"htmlBody\":" + quoteJson(safeHtml) + "," +
                    "\"body\":" + quoteJson(safeText) + "," +
                    "\"text\":" + quoteJson(safeText) + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(googleScriptUrl.trim()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                log.info("✅ Transaction Service: Email successfully delivered via Google Script Webhook to {}", maskEmail(to));
                return true;
            } else {
                log.warn("Transaction Service: Google Script Webhook returned status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Transaction Service: Google Script Webhook dispatch failed: {}", e.getMessage());
            return false;
        }
    }

    private String quoteJson(String text) {
        if (text == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

                // Non-retryable errors
                if ("AUTH_FAILURE".equals(category) || "INVALID_RECIPIENT".equals(category) || attempt >= maxAttempts) {
                    return EmailSendResult.failed(category, safeMessage, attempt);
                }

                // Transient failure retry delay
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return EmailSendResult.failed("INTERRUPTED", "Retry interrupted", attempt);
                }
            }
        }

        return EmailSendResult.failed("TIMEOUT", "Max retry attempts exhausted without successful SMTP submission", maxAttempts);
    }

    private String categorizeError(Throwable ex) {
        if (ex == null) return "UNKNOWN";

        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            if (root instanceof AuthenticationFailedException ||
                    (root.getMessage() != null && (root.getMessage().contains("535") || root.getMessage().contains("Authentication failed")))) {
                return "AUTH_FAILURE";
            }
            if (root instanceof SocketTimeoutException ||
                    (root.getMessage() != null && root.getMessage().toLowerCase().contains("timed out"))) {
                return "TIMEOUT";
            }
            if (root instanceof ConnectException || root instanceof UnknownHostException ||
                    (root.getMessage() != null && root.getMessage().toLowerCase().contains("connect"))) {
                return "CONNECTION_ERROR";
            }
            if (root instanceof AddressException) {
                return "INVALID_RECIPIENT";
            }
            root = root.getCause();
        }

        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (ex instanceof AuthenticationFailedException || msg.contains("535") || msg.contains("authentication failed") || msg.contains("username and password not accepted")) {
            return "AUTH_FAILURE";
        }
        if (ex instanceof SocketTimeoutException || msg.contains("timed out") || msg.contains("connection timed out")) {
            return "TIMEOUT";
        }
        if (ex instanceof ConnectException || ex instanceof UnknownHostException || msg.contains("could not connect") || msg.contains("connection refused")) {
            return "CONNECTION_ERROR";
        }
        if (ex instanceof AddressException || msg.contains("illegal address") || msg.contains("syntax error in address")) {
            return "INVALID_RECIPIENT";
        }

        return "UNKNOWN";
    }

    private String sanitizeMessage(String message) {
        if (message == null) return "No error detail provided";
        // Strip sensitive password tokens or long internal stacktraces
        String sanitized = message.replaceAll("password=[^,\\s]+", "password=***");
        if (sanitized.length() > 250) {
            sanitized = sanitized.substring(0, 250) + "...";
        }
        return sanitized;
    }

    private String maskEmail(String email) {
        if (email == null) return "[none]";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        return email.charAt(0) + "***" + email.charAt(atIndex - 1) + email.substring(atIndex);
    }
}
