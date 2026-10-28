package com.spendwise.transaction.service;

import com.spendwise.transaction.client.AuthClient;
import com.spendwise.transaction.client.CategoryClient;
import com.spendwise.transaction.dto.*;
import com.spendwise.transaction.entity.*;
import com.spendwise.transaction.repository.InAppNotificationRepository;
import com.spendwise.transaction.repository.NotificationDeliveryRepository;
import com.spendwise.transaction.repository.NotificationPreferenceRepository;
import com.spendwise.transaction.repository.TransactionRepository;
import com.spendwise.transaction.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private InAppNotificationRepository inAppRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryClient categoryClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private EmailService emailService;

    private EmailTemplateBuilder templateBuilder = new EmailTemplateBuilder();

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                preferenceRepository,
                deliveryRepository,
                inAppRepository,
                transactionRepository,
                categoryClient,
                authClient,
                emailService,
                templateBuilder
        );
    }

    private UserDto createTestUser(Long id) {
        return new UserDto(id, "testuser", "test@fintrack.com", "Test User", "ROLE_USER", LocalDateTime.now());
    }

    @Test
    @DisplayName("Budget alert triggers when category spending reaches 80% threshold")
    void testBudgetAlertTriggersAt80Percent() {
        Long userId = 1L;
        Long categoryId = 10L;

        Transaction expense = new Transaction();
        expense.setId(101L);
        expense.setUserId(userId);
        expense.setCategoryId(categoryId);
        expense.setAmount(new BigDecimal("8000.00"));
        expense.setType(TransactionType.EXPENSE);
        expense.setTransactionDate(LocalDate.now());

        CategoryDto cat = new CategoryDto();
        cat.setId(categoryId);
        cat.setName("Groceries");
        cat.setBudgetLimit(new BigDecimal("10000.00"));

        NotificationPreference pref = new NotificationPreference(userId);
        pref.setEmailNotificationsEnabled(true);
        pref.setBudgetAlertsEnabled(true);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(categoryClient.getCategoryById(categoryId)).thenReturn(cat);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(eq(userId), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("8500.00")); // 85% used

        when(deliveryRepository.existsByIdempotencyKey(contains("80"))).thenReturn(false);
        when(authClient.getUserById(userId)).thenReturn(createTestUser(userId));
        when(emailService.sendEmail(anyString(), anyString(), any(EmailContent.class)))
                .thenReturn(EmailSendResult.sent(1));

        notificationService.processExpenseCreated(expense);

        verify(inAppRepository, times(1)).save(any(InAppNotification.class));
        verify(emailService, times(1)).sendEmail(eq("test@fintrack.com"), eq("Test User"), any(EmailContent.class));
        verify(deliveryRepository, atLeastOnce()).save(any(NotificationDelivery.class));
    }

    @Test
    @DisplayName("Threshold deduplication: duplicate threshold alerts in the same period are skipped")
    void testThresholdDeduplicationInSamePeriod() {
        Long userId = 1L;
        Long categoryId = 10L;

        Transaction expense = new Transaction();
        expense.setId(102L);
        expense.setUserId(userId);
        expense.setCategoryId(categoryId);
        expense.setAmount(new BigDecimal("500.00"));
        expense.setType(TransactionType.EXPENSE);
        expense.setTransactionDate(LocalDate.now());

        CategoryDto cat = new CategoryDto();
        cat.setId(categoryId);
        cat.setName("Groceries");
        cat.setBudgetLimit(new BigDecimal("10000.00"));

        NotificationPreference pref = new NotificationPreference(userId);
        pref.setEmailNotificationsEnabled(true);
        pref.setBudgetAlertsEnabled(true);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(categoryClient.getCategoryById(categoryId)).thenReturn(cat);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(eq(userId), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("8800.00")); // Still in 80% bucket

        // Simulate that the 80% alert was already sent for this period
        when(deliveryRepository.existsByIdempotencyKey(contains("80"))).thenReturn(true);

        notificationService.processExpenseCreated(expense);

        // Neither in-app nor email should be re-dispatched for duplicate threshold
        verify(inAppRepository, never()).save(any(InAppNotification.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), any(EmailContent.class));
    }

    @Test
    @DisplayName("User preference opt-out: no email sent when budget alerts or email notifications are disabled")
    void testUserPreferencesGatingForBudgetAlert() {
        Long userId = 1L;
        Long categoryId = 10L;

        Transaction expense = new Transaction();
        expense.setId(103L);
        expense.setUserId(userId);
        expense.setCategoryId(categoryId);
        expense.setAmount(new BigDecimal("8000.00"));
        expense.setType(TransactionType.EXPENSE);
        expense.setTransactionDate(LocalDate.now());

        CategoryDto cat = new CategoryDto();
        cat.setId(categoryId);
        cat.setName("Groceries");
        cat.setBudgetLimit(new BigDecimal("10000.00"));

        // User disabled email notifications
        NotificationPreference pref = new NotificationPreference(userId);
        pref.setEmailNotificationsEnabled(false);
        pref.setBudgetAlertsEnabled(false);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(categoryClient.getCategoryById(categoryId)).thenReturn(cat);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(eq(userId), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("9000.00"));
        when(deliveryRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        notificationService.processExpenseCreated(expense);

        // In-app notification still created
        verify(inAppRepository, times(1)).save(any(InAppNotification.class));
        // Email service NEVER called
        verify(emailService, never()).sendEmail(anyString(), anyString(), any(EmailContent.class));
    }

    @Test
    @DisplayName("Savings goal milestone triggers email and prevents duplicate milestone emails")
    void testSavingsGoalMilestoneTriggerAndDeduplication() {
        Long userId = 2L;

        SavingsGoalEventRequest req = new SavingsGoalEventRequest();
        req.setGoalId("goal-123");
        req.setGoalName("Europe Vacation");
        req.setContributionAmount(new BigDecimal("25000.00"));
        req.setCurrentAmount(new BigDecimal("50000.00"));
        req.setTargetAmount(new BigDecimal("100000.00")); // 50% milestone!
        req.setProgressPercentage(50);
        req.setEventType("CONTRIBUTION");

        NotificationPreference pref = new NotificationPreference(userId);
        pref.setEmailNotificationsEnabled(true);
        pref.setSavingsGoalMilestonesEnabled(true);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(authClient.getUserById(userId)).thenReturn(createTestUser(userId));
        // First time: milestone not sent yet
        when(deliveryRepository.existsByIdempotencyKey(contains("goal-milestone-2-goal-123-50"))).thenReturn(false);
        when(emailService.sendEmail(anyString(), anyString(), any(EmailContent.class)))
                .thenReturn(EmailSendResult.sent(1));

        notificationService.processSavingsGoalEvent(userId, req);

        // Verifies milestone email was sent
        verify(emailService, atLeastOnce()).sendEmail(eq("test@fintrack.com"), eq("Test User"), any(EmailContent.class));
    }

    @Test
    @DisplayName("Test email rate limiting: throws HTTP 429 when recent test email count exceeds limit")
    void testTestEmailRateLimiting() {
        Long userId = 3L;
        // Simulate 3 recent test emails already sent in the last hour
        when(deliveryRepository.countByUserIdAndNotificationTypeAndCreatedAtAfter(eq(userId), eq("TEST_EMAIL"), any()))
                .thenReturn(3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.sendTestEmail(userId, "Testing rate limit")
        );

        assertEquals(429, ex.getStatusCode().value());
        verify(emailService, never()).sendEmail(anyString(), anyString(), any(EmailContent.class));
    }

    @Test
    @DisplayName("Safe fault tolerance: email failure does not throw or crash expense processing")
    void testEmailFailureDoesNotCrashProcessing() {
        Long userId = 1L;
        Long categoryId = 10L;

        Transaction expense = new Transaction();
        expense.setId(105L);
        expense.setUserId(userId);
        expense.setCategoryId(categoryId);
        expense.setAmount(new BigDecimal("15000.00"));
        expense.setType(TransactionType.EXPENSE);
        expense.setTransactionDate(LocalDate.now());

        CategoryDto cat = new CategoryDto();
        cat.setId(categoryId);
        cat.setName("Luxury");
        cat.setBudgetLimit(new BigDecimal("10000.00"));

        NotificationPreference pref = new NotificationPreference(userId);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(categoryClient.getCategoryById(categoryId)).thenReturn(cat);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("15000.00"));
        when(deliveryRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(authClient.getUserById(userId)).thenReturn(createTestUser(userId));

        // Email service throws an unexpected runtime exception
        when(emailService.sendEmail(anyString(), anyString(), any(EmailContent.class)))
                .thenThrow(new RuntimeException("Simulated SMTP network outage"));

        // Must complete without throwing exception
        assertDoesNotThrow(() -> notificationService.processExpenseCreated(expense));
    }
}
