package com.spendwise.transaction.controller;

import com.spendwise.transaction.dto.*;
import com.spendwise.transaction.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
    }

    // ==========================================
    // In-App Notifications Endpoints
    // ==========================================

    @GetMapping
    public ResponseEntity<List<InAppNotificationDto>> getNotifications(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @RequestParam(value = "category", required = false, defaultValue = "ALL") String category) {
        validateUser(userId);
        List<InAppNotificationDto> list = notificationService.getInAppNotifications(userId, category);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId) {
        validateUser(userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @PathVariable("id") String id) {
        validateUser(userId);
        notificationService.markAsRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId) {
        validateUser(userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismissNotification(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @PathVariable("id") String id) {
        validateUser(userId);
        notificationService.dismissNotification(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAllNotifications(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId) {
        validateUser(userId);
        notificationService.clearAllNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Notification Preferences Endpoints
    // ==========================================

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> getPreferences(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId) {
        validateUser(userId);
        NotificationPreferenceDto prefs = notificationService.getPreferences(userId);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @RequestBody NotificationPreferenceDto dto) {
        validateUser(userId);
        NotificationPreferenceDto updated = notificationService.updatePreferences(userId, dto);
        return ResponseEntity.ok(updated);
    }

    // ==========================================
    // Delivery Tracking History Endpoint
    // ==========================================

    @GetMapping("/deliveries")
    public ResponseEntity<List<NotificationDeliveryDto>> getDeliveries(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId) {
        validateUser(userId);
        List<NotificationDeliveryDto> deliveries = notificationService.getDeliveryHistory(userId);
        return ResponseEntity.ok(deliveries);
    }

    // ==========================================
    // Development-Only Test Email Endpoint
    // ==========================================

    @PostMapping("/test-email")
    public ResponseEntity<EmailSendResult> sendTestEmail(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @RequestBody(required = false) TestEmailRequest request) {
        validateUser(userId);
        String note = request != null ? request.getCustomNote() : null;
        EmailSendResult result = notificationService.sendTestEmail(userId, note);
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // Savings Goal Event Dispatch
    // ==========================================

    @PostMapping("/events/goal")
    public ResponseEntity<Map<String, String>> triggerGoalEvent(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @Valid @RequestBody SavingsGoalEventRequest request) {
        validateUser(userId);
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Savings goal notifications require a persisted savings goal event");
    }

    // ==========================================
    // Trigger Monthly Summary
    // ==========================================

    @PostMapping("/monthly-summary")
    public ResponseEntity<Map<String, String>> triggerMonthlySummary(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) Long userId,
            @RequestParam(value = "yearMonth", required = false) String yearMonthStr) {
        validateUser(userId);
        YearMonth ym = (yearMonthStr != null && !yearMonthStr.isBlank())
                ? YearMonth.parse(yearMonthStr.trim())
                : YearMonth.now().minusMonths(1);
        notificationService.processMonthlyFinancialSummary(userId, ym);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "ACCEPTED", "period", ym.toString()));
    }
}
