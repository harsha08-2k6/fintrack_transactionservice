package com.spendwise.transaction.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "budget_alerts_enabled", nullable = false)
    private boolean budgetAlertsEnabled = true;

    @Column(name = "expense_confirmation_enabled", nullable = false)
    private boolean expenseConfirmationEnabled = false;

    @Column(name = "income_notification_enabled", nullable = false)
    private boolean incomeNotificationEnabled = false;

    @Column(name = "savings_goal_milestones_enabled", nullable = false)
    private boolean savingsGoalMilestonesEnabled = true;

    @Column(name = "goal_deadline_reminders_enabled", nullable = false)
    private boolean goalDeadlineRemindersEnabled = true;

    @Column(name = "monthly_summary_enabled", nullable = false)
    private boolean monthlySummaryEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NotificationPreference() {}

    public NotificationPreference(Long userId) {
        this.userId = userId;
        this.emailNotificationsEnabled = true;
        this.budgetAlertsEnabled = true;
        this.expenseConfirmationEnabled = false;
        this.incomeNotificationEnabled = false;
        this.savingsGoalMilestonesEnabled = true;
        this.goalDeadlineRemindersEnabled = true;
        this.monthlySummaryEnabled = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public boolean isBudgetAlertsEnabled() { return budgetAlertsEnabled; }
    public void setBudgetAlertsEnabled(boolean budgetAlertsEnabled) { this.budgetAlertsEnabled = budgetAlertsEnabled; }

    public boolean isExpenseConfirmationEnabled() { return expenseConfirmationEnabled; }
    public void setExpenseConfirmationEnabled(boolean expenseConfirmationEnabled) { this.expenseConfirmationEnabled = expenseConfirmationEnabled; }

    public boolean isIncomeNotificationEnabled() { return incomeNotificationEnabled; }
    public void setIncomeNotificationEnabled(boolean incomeNotificationEnabled) { this.incomeNotificationEnabled = incomeNotificationEnabled; }

    public boolean isSavingsGoalMilestonesEnabled() { return savingsGoalMilestonesEnabled; }
    public void setSavingsGoalMilestonesEnabled(boolean savingsGoalMilestonesEnabled) { this.savingsGoalMilestonesEnabled = savingsGoalMilestonesEnabled; }

    public boolean isGoalDeadlineRemindersEnabled() { return goalDeadlineRemindersEnabled; }
    public void setGoalDeadlineRemindersEnabled(boolean goalDeadlineRemindersEnabled) { this.goalDeadlineRemindersEnabled = goalDeadlineRemindersEnabled; }

    public boolean isMonthlySummaryEnabled() { return monthlySummaryEnabled; }
    public void setMonthlySummaryEnabled(boolean monthlySummaryEnabled) { this.monthlySummaryEnabled = monthlySummaryEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
