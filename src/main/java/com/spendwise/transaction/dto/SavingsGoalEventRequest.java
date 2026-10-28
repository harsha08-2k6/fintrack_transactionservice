package com.spendwise.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalEventRequest {

    @NotBlank(message = "Goal ID is required")
    private String goalId;

    @NotBlank(message = "Goal name is required")
    private String goalName;

    @NotNull(message = "Target amount is required")
    private BigDecimal targetAmount;

    @NotNull(message = "Current amount is required")
    private BigDecimal currentAmount;

    private BigDecimal contributionAmount;

    private Integer progressPercentage;

    private LocalDate targetDate;

    private String eventType = "CONTRIBUTION"; // CONTRIBUTION, MILESTONE, DEADLINE_APPROACHING

    public SavingsGoalEventRequest() {}

    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public BigDecimal getContributionAmount() { return contributionAmount; }
    public void setContributionAmount(BigDecimal contributionAmount) { this.contributionAmount = contributionAmount; }

    public Integer getProgressPercentage() {
        if (progressPercentage != null) return progressPercentage;
        if (targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0 && currentAmount != null) {
            return (int) Math.min(100, Math.round(currentAmount.doubleValue() / targetAmount.doubleValue() * 100));
        }
        return 0;
    }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
