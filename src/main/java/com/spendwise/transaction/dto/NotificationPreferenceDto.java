package com.spendwise.transaction.dto;

public class NotificationPreferenceDto {
    private boolean emailNotificationsEnabled = true;
    private boolean budgetAlertsEnabled = true;
    private boolean expenseConfirmationEnabled = false;
    private boolean incomeNotificationEnabled = false;
    private boolean savingsGoalMilestonesEnabled = true;
    private boolean goalDeadlineRemindersEnabled = true;
    private boolean monthlySummaryEnabled = true;

    public NotificationPreferenceDto() {}

    public NotificationPreferenceDto(boolean emailNotificationsEnabled, boolean budgetAlertsEnabled,
                                   boolean expenseConfirmationEnabled, boolean incomeNotificationEnabled,
                                   boolean savingsGoalMilestonesEnabled, boolean goalDeadlineRemindersEnabled,
                                   boolean monthlySummaryEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.budgetAlertsEnabled = budgetAlertsEnabled;
        this.expenseConfirmationEnabled = expenseConfirmationEnabled;
        this.incomeNotificationEnabled = incomeNotificationEnabled;
        this.savingsGoalMilestonesEnabled = savingsGoalMilestonesEnabled;
        this.goalDeadlineRemindersEnabled = goalDeadlineRemindersEnabled;
        this.monthlySummaryEnabled = monthlySummaryEnabled;
    }

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
}
