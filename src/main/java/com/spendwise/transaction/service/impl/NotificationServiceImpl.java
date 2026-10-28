package com.spendwise.transaction.service.impl;

import com.spendwise.transaction.client.AuthClient;
import com.spendwise.transaction.client.CategoryClient;
import com.spendwise.transaction.dto.*;
import com.spendwise.transaction.entity.*;
import com.spendwise.transaction.repository.InAppNotificationRepository;
import com.spendwise.transaction.repository.NotificationDeliveryRepository;
import com.spendwise.transaction.repository.NotificationPreferenceRepository;
import com.spendwise.transaction.repository.TransactionRepository;
import com.spendwise.transaction.service.EmailService;
import com.spendwise.transaction.service.EmailTemplateBuilder;
import com.spendwise.transaction.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final InAppNotificationRepository inAppRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryClient categoryClient;
    private final AuthClient authClient;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;

    public NotificationServiceImpl(
            NotificationPreferenceRepository preferenceRepository,
            NotificationDeliveryRepository deliveryRepository,
            InAppNotificationRepository inAppRepository,
            TransactionRepository transactionRepository,
            CategoryClient categoryClient,
            AuthClient authClient,
            EmailService emailService,
            EmailTemplateBuilder templateBuilder) {
        this.preferenceRepository = preferenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.inAppRepository = inAppRepository;
        this.transactionRepository = transactionRepository;
        this.categoryClient = categoryClient;
        this.authClient = authClient;
        this.emailService = emailService;
        this.templateBuilder = templateBuilder;
    }

    // ==========================================
    // Notification Preferences Management
    // ==========================================
    @Override
    @Transactional
    public NotificationPreferenceDto getPreferences(Long userId) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(userId)));
        return mapToDto(pref);
    }

    @Override
    @Transactional
    public NotificationPreferenceDto updatePreferences(Long userId, NotificationPreferenceDto dto) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> new NotificationPreference(userId));

        pref.setEmailNotificationsEnabled(dto.isEmailNotificationsEnabled());
        pref.setBudgetAlertsEnabled(dto.isBudgetAlertsEnabled());
        pref.setExpenseConfirmationEnabled(dto.isExpenseConfirmationEnabled());
        pref.setIncomeNotificationEnabled(dto.isIncomeNotificationEnabled());
        pref.setSavingsGoalMilestonesEnabled(dto.isSavingsGoalMilestonesEnabled());
        pref.setGoalDeadlineRemindersEnabled(dto.isGoalDeadlineRemindersEnabled());
        pref.setMonthlySummaryEnabled(dto.isMonthlySummaryEnabled());

        NotificationPreference saved = preferenceRepository.save(pref);
        log.info("Notification preferences updated for userId={}: emailEnabled={}", userId, saved.isEmailNotificationsEnabled());
        return mapToDto(saved);
    }

    // ==========================================
    // In-App Notifications
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<InAppNotificationDto> getInAppNotifications(Long userId, String category) {
        List<InAppNotification> list;
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category.trim())) {
            list = inAppRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category.trim().toUpperCase());
        } else {
            list = inAppRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return list.stream().map(this::mapInAppToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return inAppRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, String id) {
        inAppRepository.findByIdAndUserId(id, userId).ifPresent(n -> {
            n.setRead(true);
            inAppRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        inAppRepository.markAllAsReadForUser(userId);
    }

    @Override
    @Transactional
    public void dismissNotification(Long userId, String id) {
        inAppRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    @Transactional
    public void clearAllNotifications(Long userId) {
        inAppRepository.deleteByUserId(userId);
    }

    // ==========================================
    // Delivery Tracking History
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeliveryDto> getDeliveryHistory(Long userId) {
        return deliveryRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapDeliveryToDto)
                .collect(Collectors.toList());
    }

    // ==========================================
    // Development-Only Test Email
    // ==========================================
    @Override
    @Transactional
    public EmailSendResult sendTestEmail(Long userId, String customNote) {
        // Enforce rate limit: max 3 test emails per hour per user
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentTests = deliveryRepository.countByUserIdAndNotificationTypeAndCreatedAtAfter(userId, "TEST_EMAIL", oneHourAgo);
        if (recentTests >= 3) {
            log.warn("Test email rate limit exceeded for userId={} (count={})", userId, recentTests);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Test email rate limit exceeded. Max 3 requests per hour.");
        }

        // Fetch verified user details from Auth Service
        UserDto user = resolveUser(userId);
        String recipientEmail = user.getEmail();
        String recipientName = user.getFullName() != null ? user.getFullName() : user.getUsername();

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User account has no verified email address configured.");
        }

        String eventId = "test-" + System.currentTimeMillis();
        String idempotencyKey = "test-email-" + userId + "-" + System.currentTimeMillis();

        NotificationDelivery delivery = new NotificationDelivery(userId, eventId, "TEST_EMAIL", recipientEmail, idempotencyKey);
        deliveryRepository.save(delivery);

        EmailContent content = templateBuilder.buildTestEmail(recipientName, recipientEmail, customNote);
        EmailSendResult result = emailService.sendEmail(recipientEmail, recipientName, content);

        delivery.setDeliveryStatus(result.getStatus());
        delivery.setAttemptCount(result.getAttempts());
        delivery.setErrorCategory(result.getErrorCategory());
        delivery.setErrorMessage(result.getErrorMessage());
        delivery.setLastAttemptAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        // Record in-app notification confirming diagnostic trigger
        saveInAppNotification(userId, "Test Email Executed",
                "Gmail SMTP test diagnostic submitted to " + recipientEmail + ". Status: " + result.getStatus(),
                "SECURITY", result.isSuccess() ? "SUCCESS" : "WARNING", "settings", null);

        return result;
    }

    // ==========================================
    // Event: Expense Created -> Budget Alerts & Confirmations
    // ==========================================
    @Override
    @Async("emailExecutor")
    public void processExpenseCreated(Transaction expense) {
        try {
            Long userId = expense.getUserId();
            NotificationPreference pref = getOrCreatePreference(userId);

            // 1. Expense confirmation email (if opted-in)
            if (pref.isEmailNotificationsEnabled() && pref.isExpenseConfirmationEnabled()) {
                sendExpenseConfirmationEmail(expense);
            }

            // 2. Budget threshold alerts check
            if (expense.getCategoryId() != null && expense.getCategoryId() > 0) {
                checkCategoryBudgetThresholds(expense, pref);
            }

        } catch (Exception ex) {
            // Decoupled: Email or notification failure must NEVER fail the transaction
            log.error("Error processing expense notifications for expenseId={}: {}", expense.getId(), ex.getMessage(), ex);
        }
    }

    private void sendExpenseConfirmationEmail(Transaction expense) {
        Long userId = expense.getUserId();
        UserDto user = resolveUser(userId);
        if (user.getEmail() == null || user.getEmail().isBlank()) return;

        String idempotencyKey = "exp-conf-" + expense.getId();
        if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) return;

        String categoryName = "Expense";
        if (expense.getCategoryId() != null) {
            try {
                CategoryDto cat = categoryClient.getCategoryById(expense.getCategoryId());
                if (cat != null && cat.getName() != null) categoryName = cat.getName();
            } catch (Exception ignored) {}
        }

        NotificationDelivery delivery = new NotificationDelivery(userId, String.valueOf(expense.getId()),
                "EXPENSE_CONFIRMATION", user.getEmail(), idempotencyKey);
        deliveryRepository.save(delivery);

        EmailContent content = templateBuilder.buildExpenseConfirmation(
                user.getFullName(), expense.getTitle(), expense.getAmount(),
                categoryName, expense.getTransactionDate(), expense.getPaymentMethod(), expense.getDescription());

        EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
        updateDelivery(delivery, result);
    }

    private void checkCategoryBudgetThresholds(Transaction expense, NotificationPreference pref) {
        Long userId = expense.getUserId();
        Long categoryId = expense.getCategoryId();

        CategoryDto category;
        try {
            category = categoryClient.getCategoryById(categoryId);
        } catch (Exception e) {
            log.warn("Could not retrieve category for budget check categoryId={}: {}", categoryId, e.getMessage());
            return;
        }

        if (category == null || category.getBudgetLimit() == null || category.getBudgetLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return; // No budget cap configured for this category
        }

        BigDecimal limit = category.getBudgetLimit();

        // Calculate monthly spend for this category in the expense's transaction period
        LocalDate date = expense.getTransactionDate() != null ? expense.getTransactionDate() : LocalDate.now();
        YearMonth ym = YearMonth.from(date);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal currentMonthSpend = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, start, end);
        if (currentMonthSpend == null) currentMonthSpend = BigDecimal.ZERO;

        BigDecimal remaining = limit.subtract(currentMonthSpend);
        String periodKey = ym.toString(); // e.g. "2026-10"

        // Determine if a threshold has been triggered
        String thresholdTrigger = null;
        String alertLevel = null;
        String alertTitle = null;
        String alertMessage = null;

        if (currentMonthSpend.compareTo(limit) > 0) {
            thresholdTrigger = "exceeded";
            alertLevel = "CRITICAL";
            alertTitle = category.getName() + " budget exceeded!";
            alertMessage = String.format("Spending reached %s against your %s limit (%s over budget for %s).",
                    templateBuilder.formatInr(currentMonthSpend),
                    templateBuilder.formatInr(limit),
                    templateBuilder.formatInr(currentMonthSpend.subtract(limit)),
                    periodKey);
        } else if (currentMonthSpend.compareTo(limit) == 0) {
            thresholdTrigger = "100";
            alertLevel = "WARNING";
            alertTitle = category.getName() + " budget 100% used";
            alertMessage = String.format("You have used 100%% of your %s budget for %s (%s total limit).",
                    category.getName(), periodKey, templateBuilder.formatInr(limit));
        } else {
            double usage = (currentMonthSpend.doubleValue() / limit.doubleValue()) * 100.0;
            if (usage >= 80.0) {
                thresholdTrigger = "80";
                alertLevel = "WARNING";
                alertTitle = category.getName() + " budget is " + Math.round(usage) + "% used";
                alertMessage = String.format("%s of %s used for %s. Only %s remaining.",
                        templateBuilder.formatInr(currentMonthSpend),
                        templateBuilder.formatInr(limit),
                        periodKey,
                        templateBuilder.formatInr(remaining));
            }
        }

        if (thresholdTrigger == null) return; // Under 80%, no threshold crossed

        // Deduplication key: specific to user, category, budget period (YYYY-MM), and threshold trigger
        String idempotencyKey = "budget-" + userId + "-" + categoryId + "-" + periodKey + "-" + thresholdTrigger;

        // Skip if this exact threshold was already notified in this budget period
        if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.debug("Budget alert deduplicated for key={}", idempotencyKey);
            return;
        }

        // 1. Create In-App Notification
        saveInAppNotification(userId, alertTitle, alertMessage, "BUDGET", alertLevel, "categories",
                String.format("{\"categoryId\":%d,\"spent\":%.2f,\"limit\":%.2f,\"period\":\"%s\"}",
                        categoryId, currentMonthSpend.doubleValue(), limit.doubleValue(), periodKey));

        // 2. Dispatch Email if enabled
        if (pref.isEmailNotificationsEnabled() && pref.isBudgetAlertsEnabled()) {
            UserDto user = resolveUser(userId);
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                NotificationDelivery delivery = new NotificationDelivery(userId, String.valueOf(expense.getId()),
                        "BUDGET_ALERT", user.getEmail(), idempotencyKey);
                deliveryRepository.save(delivery);

                EmailContent content = templateBuilder.buildBudgetAlert(
                        user.getFullName(), category.getName(), currentMonthSpend, limit, remaining, periodKey, alertLevel);

                EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
                updateDelivery(delivery, result);
            }
        }
    }

    // ==========================================
    // Event: Income Created
    // ==========================================
    @Override
    @Async("emailExecutor")
    public void processIncomeCreated(Transaction income) {
        try {
            Long userId = income.getUserId();
            NotificationPreference pref = getOrCreatePreference(userId);

            // In-app notification
            String formattedAmt = templateBuilder.formatInr(income.getAmount());
            saveInAppNotification(userId, "Income Recorded: " + income.getTitle(),
                    "Received +" + formattedAmt + " via " + income.getPaymentMethod(),
                    "TRANSACTION", "SUCCESS", "transactions", null);

            // Optional email notification if opted-in
            if (pref.isEmailNotificationsEnabled() && pref.isIncomeNotificationEnabled()) {
                UserDto user = resolveUser(userId);
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    String idempotencyKey = "inc-conf-" + income.getId();
                    if (!deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
                        NotificationDelivery delivery = new NotificationDelivery(userId, String.valueOf(income.getId()),
                                "INCOME_RECORDED", user.getEmail(), idempotencyKey);
                        deliveryRepository.save(delivery);

                        EmailContent content = templateBuilder.buildIncomeNotification(
                                user.getFullName(), income.getTitle(), income.getAmount(),
                                income.getTransactionDate(), income.getPaymentMethod());

                        EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
                        updateDelivery(delivery, result);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error processing income notification for incomeId={}: {}", income.getId(), ex.getMessage(), ex);
        }
    }

    // ==========================================
    // Event: Savings Goal Contribution / Milestones / Deadlines
    // ==========================================
    @Override
    @Async("emailExecutor")
    public void processSavingsGoalEvent(Long userId, SavingsGoalEventRequest req) {
        try {
            NotificationPreference pref = getOrCreatePreference(userId);
            UserDto user = resolveUser(userId);
            int progress = req.getProgressPercentage();

            // 1. Contribution confirmation
            if (req.getContributionAmount() != null && req.getContributionAmount().compareTo(BigDecimal.ZERO) > 0) {
                String inAppTitle = progress >= 100 ? "Goal \"" + req.getGoalName() + "\" Reached! 🎉" : req.getGoalName() + " savings updated";
                String inAppMsg = String.format("%s contribution added. Goal is now %d%% complete (%s of %s).",
                        templateBuilder.formatInr(req.getContributionAmount()),
                        progress,
                        templateBuilder.formatInr(req.getCurrentAmount()),
                        templateBuilder.formatInr(req.getTargetAmount()));

                saveInAppNotification(userId, inAppTitle, inAppMsg, "GOAL", progress >= 100 ? "SUCCESS" : "INFO", "goals", null);

                if (pref.isEmailNotificationsEnabled() && pref.isSavingsGoalMilestonesEnabled() && user.getEmail() != null) {
                    String contribKey = "goal-contrib-" + userId + "-" + req.getGoalId() + "-" + System.currentTimeMillis();
                    NotificationDelivery delivery = new NotificationDelivery(userId, req.getGoalId(), "SAVINGS_GOAL_CONTRIBUTION", user.getEmail(), contribKey);
                    deliveryRepository.save(delivery);

                    EmailContent content = templateBuilder.buildSavingsGoalContribution(
                            user.getFullName(), req.getGoalName(), req.getContributionAmount(),
                            req.getCurrentAmount(), req.getTargetAmount(), progress);

                    EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
                    updateDelivery(delivery, result);
                }
            }

            // 2. Milestone check: 25%, 50%, 75%, 100%
            int milestoneBucket = 0;
            if (progress >= 100) milestoneBucket = 100;
            else if (progress >= 75) milestoneBucket = 75;
            else if (progress >= 50) milestoneBucket = 50;
            else if (progress >= 25) milestoneBucket = 25;

            if (milestoneBucket > 0) {
                String milestoneKey = "goal-milestone-" + userId + "-" + req.getGoalId() + "-" + milestoneBucket;
                if (!deliveryRepository.existsByIdempotencyKey(milestoneKey)) {
                    // Record in-app notification
                    String milestoneTitle = milestoneBucket == 100
                            ? "Goal \"" + req.getGoalName() + "\" 100% Accomplished! 🏆"
                            : "Milestone: \"" + req.getGoalName() + "\" is " + milestoneBucket + "% Complete! 🎯";

                    saveInAppNotification(userId, milestoneTitle,
                            "You have reached " + milestoneBucket + "% of your target (" + templateBuilder.formatInr(req.getCurrentAmount()) + " saved).",
                            "GOAL", "SUCCESS", "goals", null);

                    if (pref.isEmailNotificationsEnabled() && pref.isSavingsGoalMilestonesEnabled() && user.getEmail() != null) {
                        NotificationDelivery delivery = new NotificationDelivery(userId, req.getGoalId(), "SAVINGS_GOAL_MILESTONE", user.getEmail(), milestoneKey);
                        deliveryRepository.save(delivery);

                        EmailContent content = templateBuilder.buildSavingsGoalMilestone(
                                user.getFullName(), req.getGoalName(), milestoneBucket, req.getCurrentAmount(), req.getTargetAmount());

                        EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
                        updateDelivery(delivery, result);
                    }
                }
            }

            // 3. Deadline Approaching Reminder
            if (req.getTargetDate() != null && req.getTargetAmount().compareTo(req.getCurrentAmount()) > 0) {
                LocalDate today = LocalDate.now();
                long daysRemaining = ChronoUnit.DAYS.between(today, req.getTargetDate());
                if (daysRemaining >= 0 && daysRemaining <= 7) {
                    String deadlineKey = "goal-deadline-" + userId + "-" + req.getGoalId() + "-" + req.getTargetDate() + "-" + daysRemaining;
                    if (!deliveryRepository.existsByIdempotencyKey(deadlineKey)) {
                        saveInAppNotification(userId, "Deadline Approaching: " + req.getGoalName(),
                                "Only " + daysRemaining + " days remaining until " + req.getTargetDate() + " deadline.",
                                "GOAL", "WARNING", "goals", null);

                        if (pref.isEmailNotificationsEnabled() && pref.isGoalDeadlineRemindersEnabled() && user.getEmail() != null) {
                            NotificationDelivery delivery = new NotificationDelivery(userId, req.getGoalId(), "GOAL_DEADLINE_APPROACHING", user.getEmail(), deadlineKey);
                            deliveryRepository.save(delivery);

                            EmailContent content = templateBuilder.buildGoalDeadlineApproaching(
                                    user.getFullName(), req.getGoalName(), req.getTargetDate(), daysRemaining,
                                    req.getCurrentAmount(), req.getTargetAmount(), progress);

                            EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
                            updateDelivery(delivery, result);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            log.error("Error processing savings goal event for userId={}: {}", userId, ex.getMessage(), ex);
        }
    }

    // ==========================================
    // Event: Monthly Financial Summary
    // ==========================================
    @Override
    @Async("emailExecutor")
    public void processMonthlyFinancialSummary(Long userId, YearMonth targetYm) {
        try {
            YearMonth ym = targetYm != null ? targetYm : YearMonth.now().minusMonths(1);
            NotificationPreference pref = getOrCreatePreference(userId);
            if (!pref.isEmailNotificationsEnabled() || !pref.isMonthlySummaryEnabled()) {
                log.info("Monthly summary email skipped for userId={}: user opted out", userId);
                return;
            }

            UserDto user = resolveUser(userId);
            if (user.getEmail() == null || user.getEmail().isBlank()) return;

            String periodStr = ym.toString();
            String idempotencyKey = "summary-" + userId + "-" + periodStr;
            if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Monthly summary already generated for userId={}, period={}", userId, periodStr);
                return;
            }

            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, start, end);
            if (totalIncome == null) totalIncome = BigDecimal.ZERO;

            BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, start, end);
            if (totalExpense == null) totalExpense = BigDecimal.ZERO;

            BigDecimal netCashFlow = totalIncome.subtract(totalExpense);
            Double savingsRate = 0.0;
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                savingsRate = netCashFlow.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
            }

            // Category breakdown for summary
            List<Transaction> expenses = transactionRepository.findByUserIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                    userId, TransactionType.EXPENSE, start, end);

            Map<Long, BigDecimal> catTotals = new HashMap<>();
            for (Transaction e : expenses) {
                if (e.getCategoryId() != null) {
                    catTotals.put(e.getCategoryId(), catTotals.getOrDefault(e.getCategoryId(), BigDecimal.ZERO).add(e.getAmount()));
                }
            }

            List<CategorySummaryItem> categoryItems = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> entry : catTotals.entrySet()) {
                String catName = "Category " + entry.getKey();
                try {
                    CategoryDto cd = categoryClient.getCategoryById(entry.getKey());
                    if (cd != null && cd.getName() != null) catName = cd.getName();
                } catch (Exception ignored) {}

                double catPct = totalExpense.compareTo(BigDecimal.ZERO) > 0
                        ? (entry.getValue().doubleValue() / totalExpense.doubleValue()) * 100 : 0.0;

                categoryItems.add(new CategorySummaryItem(catName, entry.getValue(), catPct));
            }
            categoryItems.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

            NotificationDelivery delivery = new NotificationDelivery(userId, periodStr, "MONTHLY_SUMMARY", user.getEmail(), idempotencyKey);
            deliveryRepository.save(delivery);

            EmailContent content = templateBuilder.buildMonthlySummary(
                    user.getFullName(), periodStr, totalIncome, totalExpense, netCashFlow, savingsRate, categoryItems);

            EmailSendResult result = emailService.sendEmail(user.getEmail(), user.getFullName(), content);
            updateDelivery(delivery, result);

            saveInAppNotification(userId, "Monthly Financial Statement Generated: " + periodStr,
                    String.format("Net Cash Flow: %s (Savings Rate: %.1f%%). Reconciled statement sent to %s.",
                            templateBuilder.formatInr(netCashFlow), savingsRate, user.getEmail()),
                    "SUMMARY", "INFO", "reports", null);

        } catch (Exception ex) {
            log.error("Error processing monthly summary for userId={}: {}", userId, ex.getMessage(), ex);
        }
    }

    // ==========================================
    // Internal Helper Methods
    // ==========================================
    private UserDto resolveUser(Long userId) {
        try {
            UserDto u = authClient.getUserById(userId);
            if (u != null) return u;
        } catch (Exception ex) {
            log.warn("AuthClient lookup failed for userId {}: {}", userId, ex.getMessage());
        }
        // Fallback default user
        return new UserDto(userId, "user" + userId, "user" + userId + "@fintrack.com", "FinTrack User", "ROLE_USER", LocalDateTime.now());
    }

    private NotificationPreference getOrCreatePreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(userId)));
    }

    private void saveInAppNotification(Long userId, String title, String message, String category,
                                      String severity, String actionUrl, String metadataJson) {
        try {
            String id = "notif-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4);
            InAppNotification n = new InAppNotification(id, userId, title, message, category, severity, actionUrl, metadataJson);
            inAppRepository.save(n);
        } catch (Exception e) {
            log.warn("Could not save in-app notification: {}", e.getMessage());
        }
    }

    private void updateDelivery(NotificationDelivery delivery, EmailSendResult result) {
        delivery.setDeliveryStatus(result.getStatus());
        delivery.setAttemptCount(result.getAttempts());
        delivery.setErrorCategory(result.getErrorCategory());
        delivery.setErrorMessage(result.getErrorMessage());
        delivery.setLastAttemptAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    private NotificationPreferenceDto mapToDto(NotificationPreference p) {
        return new NotificationPreferenceDto(
                p.isEmailNotificationsEnabled(),
                p.isBudgetAlertsEnabled(),
                p.isExpenseConfirmationEnabled(),
                p.isIncomeNotificationEnabled(),
                p.isSavingsGoalMilestonesEnabled(),
                p.isGoalDeadlineRemindersEnabled(),
                p.isMonthlySummaryEnabled()
        );
    }

    private InAppNotificationDto mapInAppToDto(InAppNotification n) {
        return new InAppNotificationDto(
                n.getId(),
                n.getUserId(),
                n.getTitle(),
                n.getMessage(),
                n.getCategory(),
                n.getSeverity(),
                n.isRead(),
                n.getActionUrl(),
                n.getMetadataJson(),
                n.getCreatedAt()
        );
    }

    private NotificationDeliveryDto mapDeliveryToDto(NotificationDelivery d) {
        NotificationDeliveryDto dto = new NotificationDeliveryDto();
        dto.setId(d.getId());
        dto.setUserId(d.getUserId());
        dto.setEventId(d.getEventId());
        dto.setNotificationType(d.getNotificationType());
        dto.setRecipientEmail(d.getRecipientEmail());
        dto.setDeliveryStatus(d.getDeliveryStatus());
        dto.setAttemptCount(d.getAttemptCount());
        dto.setErrorCategory(d.getErrorCategory());
        dto.setErrorMessage(d.getErrorMessage());
        dto.setIdempotencyKey(d.getIdempotencyKey());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setLastAttemptAt(d.getLastAttemptAt());
        return dto;
    }
}
