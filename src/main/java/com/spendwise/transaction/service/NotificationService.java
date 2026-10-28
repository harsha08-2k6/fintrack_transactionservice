package com.spendwise.transaction.service;

import com.spendwise.transaction.dto.*;
import com.spendwise.transaction.entity.Transaction;

import java.time.YearMonth;
import java.util.List;

public interface NotificationService {

    NotificationPreferenceDto getPreferences(Long userId);

    NotificationPreferenceDto updatePreferences(Long userId, NotificationPreferenceDto dto);

    List<InAppNotificationDto> getInAppNotifications(Long userId, String category);

    long getUnreadCount(Long userId);

    void markAsRead(Long userId, String id);

    void markAllAsRead(Long userId);

    void dismissNotification(Long userId, String id);

    void clearAllNotifications(Long userId);

    List<NotificationDeliveryDto> getDeliveryHistory(Long userId);

    EmailSendResult sendTestEmail(Long userId, String customNote);

    void processExpenseCreated(Transaction expense);

    void processIncomeCreated(Transaction income);

    void processSavingsGoalEvent(Long userId, SavingsGoalEventRequest request);

    void processMonthlyFinancialSummary(Long userId, YearMonth yearMonth);
}
