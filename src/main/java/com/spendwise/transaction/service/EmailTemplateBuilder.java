package com.spendwise.transaction.service;

import com.spendwise.transaction.dto.CategorySummaryItem;
import com.spendwise.transaction.dto.EmailContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class EmailTemplateBuilder {

    @Value("${fintrack.mail.app-url:http://localhost:5173}")
    private String appUrl;

    private static final Locale INDIA_LOCALE = new Locale("en", "IN");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Escape user-provided inputs to prevent HTML injection / XSS.
     */
    public String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Format currency in Indian Rupees (INR) format (e.g. ₹15,400.00).
     */
    public String formatInr(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance(INDIA_LOCALE);
            String formatted = format.format(amount);
            // Replace standard INR symbol if needed to guarantee ₹
            if (!formatted.contains("₹") && !formatted.contains("Rs")) {
                formatted = "₹" + formatted.replaceAll("[^0-9,.]", "").trim();
            }
            return formatted;
        } catch (Exception e) {
            return "₹" + String.format("%.2f", amount.doubleValue());
        }
    }

    // ==========================================
    // 1. Budget Alert Template (80%, 100%, Over-budget)
    // ==========================================
    public EmailContent buildBudgetAlert(String recipientName, String categoryName,
                                        BigDecimal spent, BigDecimal limit,
                                        BigDecimal remaining, String period,
                                        String alertLevel) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeCategory = escapeHtml(categoryName);
        String safePeriod = escapeHtml(period);

        String badgeBg;
        String badgeColor;
        String badgeBorder;
        String alertTitle;
        String alertAdvice;

        if ("CRITICAL".equalsIgnoreCase(alertLevel) || spent.compareTo(limit) > 0) {
            badgeBg = "#FEF2F2";
            badgeColor = "#DC2626";
            badgeBorder = "#FECACA";
            alertTitle = "Budget Limit Exceeded: " + safeCategory;
            alertAdvice = "Your spending in " + safeCategory + " has exceeded the allocated limit by " +
                    formatInr(spent.subtract(limit)) + ". Review upcoming transactions to rebalance your budget.";
        } else if ("LIMIT_REACHED".equalsIgnoreCase(alertLevel) || spent.compareTo(limit) == 0) {
            badgeBg = "#FFFBEB";
            badgeColor = "#D97706";
            badgeBorder = "#FDE68A";
            alertTitle = "100% of " + safeCategory + " Budget Reached";
            alertAdvice = "You have fully utilized your monthly allocation for " + safeCategory +
                    ". Any further expenditures will put this category over budget.";
        } else {
            badgeBg = "#FFFBEB";
            badgeColor = "#D97706";
            badgeBorder = "#FDE68A";
            alertTitle = "80% Budget Threshold Reached: " + safeCategory;
            alertAdvice = "You have utilized over 80% of your allocated budget for " + safeCategory +
                    ". You have " + formatInr(remaining) + " remaining for the remainder of " + safePeriod + ".";
        }

        double percent = limit.compareTo(BigDecimal.ZERO) > 0
                ? Math.min(200, (spent.doubleValue() / limit.doubleValue()) * 100) : 100.0;

        String subject = "[FinTrack Alert] " + alertTitle;

        String html = wrapHtml(
                "Budget Threshold Alert",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: " + badgeBg + "; color: " + badgeColor + "; border: 1px solid " + badgeBorder + "; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">" +
                        escapeHtml(alertLevel) + " ALERT</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 22px; font-weight: 800; margin: 16px 0 8px;\">" + alertTitle + "</h2>" +
                        "  <p style=\"color: #475569; font-size: 15px; margin: 0;\">Hello " + safeName + ", here is your real-time budget status update.</p>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Category", safeCategory) +
                        renderRow("Budget Period", safePeriod) +
                        renderRow("Current Spending", "<strong style=\"color: #0F172A;\">" + formatInr(spent) + "</strong>") +
                        renderRow("Monthly Limit", formatInr(limit)) +
                        renderRow("Remaining Balance", "<strong style=\"color: " + (remaining.compareTo(BigDecimal.ZERO) >= 0 ? "#10B981" : "#EF4444") + ";\">" + formatInr(remaining) + "</strong>") +
                        renderRow("Utilization", String.format("%.1f%%", percent)) +
                        "  </table>" +
                        "  <div style=\"background-color: #E2E8F0; border-radius: 6px; height: 10px; margin-top: 16px; overflow: hidden;\">" +
                        "    <div style=\"background-color: " + badgeColor + "; width: " + Math.min(100, Math.round(percent)) + "%; height: 100%; border-radius: 6px;\"></div>" +
                        "  </div>" +
                        "</div>" +
                        "<p style=\"color: #334155; font-size: 14px; line-height: 1.6; margin-bottom: 24px;\">" + alertAdvice + "</p>" +
                        renderButton("View Categories & Budgets", appUrl + "/categories")
        );

        String text = "FINTRACK BUDGET ALERT\n\n" +
                "Hello " + recipientName + ",\n\n" +
                alertTitle + "\n\n" +
                "Category: " + categoryName + "\n" +
                "Period: " + period + "\n" +
                "Current Spending: " + formatInr(spent) + "\n" +
                "Monthly Limit: " + formatInr(limit) + "\n" +
                "Remaining Balance: " + formatInr(remaining) + "\n" +
                "Utilization: " + String.format("%.1f%%", percent) + "\n\n" +
                alertAdvice + "\n\n" +
                "Manage your budgets at: " + appUrl + "/categories\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 2. Expense Confirmation Template
    // ==========================================
    public EmailContent buildExpenseConfirmation(String recipientName, String title,
                                                BigDecimal amount, String categoryName,
                                                LocalDate date, String paymentMethod,
                                                String description) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeTitle = escapeHtml(title);
        String safeCategory = escapeHtml(categoryName != null ? categoryName : "Uncategorized");
        String safeMethod = escapeHtml(paymentMethod != null ? paymentMethod : "CARD");
        String safeDate = date != null ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        String safeDesc = description != null && !description.isBlank() ? escapeHtml(description) : "None provided";

        String subject = "[FinTrack] Expense Recorded: " + safeTitle + " (" + formatInr(amount) + ")";

        String html = wrapHtml(
                "Expense Confirmation",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #EEF2FF; color: #4F46E5; border: 1px solid #C7D2FE; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">EXPENSE LOGGED</span>" +
                        "  <div style=\"font-size: 32px; font-weight: 800; color: #0F172A; margin: 16px 0 6px;\">" + formatInr(amount) + "</div>" +
                        "  <p style=\"color: #64748B; font-size: 15px; margin: 0;\">" + safeTitle + "</p>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Category", safeCategory) +
                        renderRow("Transaction Date", safeDate) +
                        renderRow("Payment Method", safeMethod) +
                        renderRow("Notes", safeDesc) +
                        "  </table>" +
                        "</div>" +
                        "<p style=\"color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 24px;\">" +
                        "Hello " + safeName + ", this is a confirmation that your expense transaction was successfully persisted into your financial ledger." +
                        "</p>" +
                        renderButton("View All Transactions", appUrl + "/transactions")
        );

        String text = "FINTRACK EXPENSE CONFIRMATION\n\n" +
                "Hello " + recipientName + ",\n\n" +
                "An expense has been recorded in your FinTrack ledger:\n\n" +
                "Title: " + title + "\n" +
                "Amount: " + formatInr(amount) + "\n" +
                "Category: " + categoryName + "\n" +
                "Date: " + safeDate + "\n" +
                "Payment Method: " + paymentMethod + "\n" +
                "Notes: " + safeDesc + "\n\n" +
                "View your transaction history at: " + appUrl + "/transactions\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 3. Income Notification Template
    // ==========================================
    public EmailContent buildIncomeNotification(String recipientName, String title,
                                               BigDecimal amount, LocalDate date,
                                               String paymentMethod) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeTitle = escapeHtml(title);
        String safeDate = date != null ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        String safeMethod = escapeHtml(paymentMethod != null ? paymentMethod : "BANK_TRANSFER");

        String subject = "[FinTrack] Income Recorded: " + safeTitle + " (+" + formatInr(amount) + ")";

        String html = wrapHtml(
                "Income Inflow Recorded",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #ECFDF5; color: #059669; border: 1px solid #A7F3D0; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">INCOME INFLOW</span>" +
                        "  <div style=\"font-size: 32px; font-weight: 800; color: #10B981; margin: 16px 0 6px;\">+" + formatInr(amount) + "</div>" +
                        "  <p style=\"color: #64748B; font-size: 15px; margin: 0;\">" + safeTitle + "</p>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Source / Title", safeTitle) +
                        renderRow("Amount Added", "<strong style=\"color: #10B981;\">+" + formatInr(amount) + "</strong>") +
                        renderRow("Inflow Date", safeDate) +
                        renderRow("Deposit Channel", safeMethod) +
                        "  </table>" +
                        "</div>" +
                        "<p style=\"color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 24px;\">" +
                        "Hello " + safeName + ", your income record has been added to your cashflow calculations. Your monthly savings and liquidity projections have been updated." +
                        "</p>" +
                        renderButton("View Financial Dashboard", appUrl + "/dashboard")
        );

        String text = "FINTRACK INCOME RECORDED\n\n" +
                "Hello " + recipientName + ",\n\n" +
                "A new income entry has been saved to your account:\n\n" +
                "Source: " + title + "\n" +
                "Amount: +" + formatInr(amount) + "\n" +
                "Date: " + safeDate + "\n" +
                "Channel: " + paymentMethod + "\n\n" +
                "View your dashboard at: " + appUrl + "/dashboard\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 4. Savings Goal Contribution Template
    // ==========================================
    public EmailContent buildSavingsGoalContribution(String recipientName, String goalName,
                                                    BigDecimal contribution, BigDecimal currentAmount,
                                                    BigDecimal targetAmount, int progress) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeGoal = escapeHtml(goalName);
        BigDecimal remaining = targetAmount.subtract(currentAmount).max(BigDecimal.ZERO);

        String subject = "[FinTrack Goal] Contribution to \"" + safeGoal + "\" (" + progress + "% Complete)";

        String html = wrapHtml(
                "Savings Goal Contribution",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #ECFDF5; color: #059669; border: 1px solid #A7F3D0; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">SAVINGS CONTRIBUTION</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 22px; font-weight: 800; margin: 16px 0 6px;\">" + safeGoal + "</h2>" +
                        "  <div style=\"font-size: 28px; font-weight: 800; color: #10B981; margin: 6px 0;\">+" + formatInr(contribution) + " added</div>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Total Saved So Far", "<strong>" + formatInr(currentAmount) + "</strong>") +
                        renderRow("Target Goal Amount", formatInr(targetAmount)) +
                        renderRow("Remaining to Target", formatInr(remaining)) +
                        renderRow("Current Progress", progress + "%") +
                        "  </table>" +
                        "  <div style=\"background-color: #E2E8F0; border-radius: 6px; height: 12px; margin-top: 16px; overflow: hidden;\">" +
                        "    <div style=\"background-color: #10B981; width: " + Math.min(100, progress) + "%; height: 100%; border-radius: 6px;\"></div>" +
                        "  </div>" +
                        "</div>" +
                        "<p style=\"color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 24px;\">" +
                        "Hello " + safeName + ", your contribution has been securely logged. You are " + progress + "% of the way toward your target." +
                        "</p>" +
                        renderButton("View Savings Goals", appUrl + "/goals")
        );

        String text = "FINTRACK SAVINGS GOAL CONTRIBUTION\n\n" +
                "Hello " + recipientName + ",\n\n" +
                "A contribution of +" + formatInr(contribution) + " was made to your goal: " + goalName + "\n\n" +
                "Total Saved: " + formatInr(currentAmount) + "\n" +
                "Target Amount: " + formatInr(targetAmount) + "\n" +
                "Remaining: " + formatInr(remaining) + "\n" +
                "Progress: " + progress + "%\n\n" +
                "Manage goals at: " + appUrl + "/goals\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 5. Savings Goal Milestone Template (25%, 50%, 75%, 100%)
    // ==========================================
    public EmailContent buildSavingsGoalMilestone(String recipientName, String goalName,
                                                 int milestonePercent, BigDecimal currentAmount,
                                                 BigDecimal targetAmount) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeGoal = escapeHtml(goalName);
        boolean isComplete = milestonePercent >= 100;

        String subject = isComplete
                ? "🎉 [FinTrack Milestone] Congratulations! Goal \"" + safeGoal + "\" Reached!"
                : "🎯 [FinTrack Milestone] \"" + safeGoal + "\" is " + milestonePercent + "% Complete!";

        String title = isComplete ? "Target Accomplished! 🎉" : milestonePercent + "% Milestone Achieved! 🎯";
        String message = isComplete
                ? "Congratulations " + safeName + "! You have officially hit 100% of your savings goal target of " + formatInr(targetAmount) + "."
                : "Great news " + safeName + "! You've hit the " + milestonePercent + "% milestone for \"" + safeGoal + "\". You've saved " + formatInr(currentAmount) + " toward your target of " + formatInr(targetAmount) + ".";

        String html = wrapHtml(
                "Savings Milestone Reached",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">MILESTONE ACHIEVED</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 24px; font-weight: 800; margin: 16px 0 8px;\">" + title + "</h2>" +
                        "  <p style=\"color: #475569; font-size: 15px; margin: 0;\">" + message + "</p>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Goal Name", safeGoal) +
                        renderRow("Saved to Date", "<strong style=\"color: #10B981;\">" + formatInr(currentAmount) + "</strong>") +
                        renderRow("Total Target", formatInr(targetAmount)) +
                        renderRow("Milestone Status", "<strong style=\"color: #0F172A;\">" + milestonePercent + "% Complete</strong>") +
                        "  </table>" +
                        "  <div style=\"background-color: #E2E8F0; border-radius: 6px; height: 12px; margin-top: 16px; overflow: hidden;\">" +
                        "    <div style=\"background-color: #10B981; width: " + Math.min(100, milestonePercent) + "%; height: 100%; border-radius: 6px;\"></div>" +
                        "  </div>" +
                        "</div>" +
                        renderButton("View Your Goals", appUrl + "/goals")
        );

        String text = "FINTRACK SAVINGS MILESTONE\n\n" +
                "Hello " + recipientName + ",\n\n" +
                title + "\n\n" +
                message + "\n\n" +
                "Goal: " + goalName + "\n" +
                "Saved: " + formatInr(currentAmount) + " of " + formatInr(targetAmount) + " (" + milestonePercent + "%)\n\n" +
                "View your goals at: " + appUrl + "/goals\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 6. Goal Deadline Approaching Template
    // ==========================================
    public EmailContent buildGoalDeadlineApproaching(String recipientName, String goalName,
                                                    LocalDate deadline, long daysRemaining,
                                                    BigDecimal currentAmount, BigDecimal targetAmount,
                                                    int progress) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeGoal = escapeHtml(goalName);
        String safeDeadline = deadline != null ? deadline.format(DATE_FORMATTER) : "Upcoming";
        BigDecimal remaining = targetAmount.subtract(currentAmount).max(BigDecimal.ZERO);

        String subject = "⏳ [FinTrack Reminder] Deadline Approaching for Goal \"" + safeGoal + "\" (" + daysRemaining + " days left)";

        String html = wrapHtml(
                "Goal Deadline Reminder",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #EFF6FF; color: #1D4ED8; border: 1px solid #BFDBFE; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">DEADLINE REMINDER</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 22px; font-weight: 800; margin: 16px 0 8px;\">" + safeGoal + " Target Date Approaching</h2>" +
                        "  <p style=\"color: #475569; font-size: 15px; margin: 0;\">You have <strong style=\"color: #1D4ED8;\">" + daysRemaining + " days</strong> remaining until the target date (" + safeDeadline + ").</p>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Goal Name", safeGoal) +
                        renderRow("Target Date", safeDeadline) +
                        renderRow("Days Remaining", daysRemaining + " days") +
                        renderRow("Current Balance", formatInr(currentAmount)) +
                        renderRow("Target Balance", formatInr(targetAmount)) +
                        renderRow("Remaining Gap", "<strong style=\"color: #EF4444;\">" + formatInr(remaining) + "</strong>") +
                        renderRow("Progress", progress + "%") +
                        "  </table>" +
                        "</div>" +
                        "<p style=\"color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 24px;\">" +
                        "Hello " + safeName + ", making an additional contribution now will keep you on track to meet your target before " + safeDeadline + "." +
                        "</p>" +
                        renderButton("Add Goal Contribution", appUrl + "/goals")
        );

        String text = "FINTRACK GOAL DEADLINE REMINDER\n\n" +
                "Hello " + recipientName + ",\n\n" +
                "Your savings goal \"" + goalName + "\" has a target deadline approaching on " + safeDeadline + " (" + daysRemaining + " days remaining).\n\n" +
                "Current Balance: " + formatInr(currentAmount) + "\n" +
                "Target Balance: " + formatInr(targetAmount) + "\n" +
                "Remaining Gap: " + formatInr(remaining) + "\n" +
                "Progress: " + progress + "%\n\n" +
                "Contribute at: " + appUrl + "/goals\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 7. Monthly Financial Summary Template
    // ==========================================
    public EmailContent buildMonthlySummary(String recipientName, String yearMonth,
                                           BigDecimal totalIncome, BigDecimal totalExpenses,
                                           BigDecimal netCashFlow, Double savingsRate,
                                           List<CategorySummaryItem> categories) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safePeriod = escapeHtml(yearMonth);
        double rate = savingsRate != null ? savingsRate : 0.0;

        String subject = "📊 [FinTrack Report] Monthly Financial Summary - " + safePeriod;

        StringBuilder catRows = new StringBuilder();
        StringBuilder catText = new StringBuilder();
        if (categories != null && !categories.isEmpty()) {
            for (CategorySummaryItem item : categories) {
                String safeCat = escapeHtml(item.getCategoryName());
                catRows.append("<tr>")
                        .append("<td style=\"padding: 8px 0; color: #334155; font-size: 14px;\">").append(safeCat).append("</td>")
                        .append("<td style=\"padding: 8px 0; color: #0F172A; font-weight: 700; text-align: right; font-size: 14px;\">").append(formatInr(item.getAmount())).append("</td>")
                        .append("<td style=\"padding: 8px 0; color: #64748B; text-align: right; font-size: 13px;\">").append(String.format("%.1f%%", item.getPercentage())).append("</td>")
                        .append("</tr>");

                catText.append("- ").append(item.getCategoryName()).append(": ")
                        .append(formatInr(item.getAmount())).append(" (")
                        .append(String.format("%.1f%%", item.getPercentage())).append(")\n");
            }
        } else {
            catRows.append("<tr><td colspan=\"3\" style=\"padding: 12px 0; color: #94A3B8; text-align: center; font-size: 13px;\">No category breakdown data available</td></tr>");
            catText.append("No category breakdown data available.\n");
        }

        String html = wrapHtml(
                "Monthly Financial Statement",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #F1F5F9; color: #334155; border: 1px solid #CBD5E1; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">MONTHLY STATEMENT</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 24px; font-weight: 800; margin: 16px 0 6px;\">Financial Summary: " + safePeriod + "</h2>" +
                        "  <p style=\"color: #64748B; font-size: 15px; margin: 0;\">Hello " + safeName + ", here is your reconciled monthly statement.</p>" +
                        "</div>" +
                        "<div style=\"display: table; width: 100%; margin-bottom: 24px;\">" +
                        "  <div style=\"display: table-row;\">" +
                        "    <div style=\"display: table-cell; width: 50%; padding-right: 8px;\">" +
                        "      <div style=\"background-color: #ECFDF5; border: 1px solid #A7F3D0; border-radius: 12px; padding: 16px; text-align: center;\">" +
                        "        <div style=\"font-size: 12px; color: #047857; font-weight: 700; text-transform: uppercase;\">Total Inflow</div>" +
                        "        <div style=\"font-size: 20px; font-weight: 800; color: #10B981; margin-top: 6px;\">+" + formatInr(totalIncome) + "</div>" +
                        "      </div>" +
                        "    </div>" +
                        "    <div style=\"display: table-cell; width: 50%; padding-left: 8px;\">" +
                        "      <div style=\"background-color: #FEF2F2; border: 1px solid #FECACA; border-radius: 12px; padding: 16px; text-align: center;\">" +
                        "        <div style=\"font-size: 12px; color: #B91C1C; font-weight: 700; text-transform: uppercase;\">Total Outflow</div>" +
                        "        <div style=\"font-size: 20px; font-weight: 800; color: #EF4444; margin-top: 6px;\">" + formatInr(totalExpenses) + "</div>" +
                        "      </div>" +
                        "    </div>" +
                        "  </div>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Net Cash Flow", "<strong style=\"color: " + (netCashFlow.compareTo(BigDecimal.ZERO) >= 0 ? "#10B981" : "#EF4444") + ";\">" + formatInr(netCashFlow) + "</strong>") +
                        renderRow("Savings Rate", "<strong style=\"color: #0F172A;\">" + String.format("%.1f%%", rate) + "</strong>") +
                        "  </table>" +
                        "</div>" +
                        "<h3 style=\"color: #0F172A; font-size: 16px; font-weight: 700; margin: 20px 0 10px;\">Category Spending Breakdown</h3>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 16px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        catRows.toString() +
                        "  </table>" +
                        "</div>" +
                        renderButton("View Full Analytics Report", appUrl + "/reports")
        );

        String text = "FINTRACK MONTHLY FINANCIAL SUMMARY (" + yearMonth + ")\n\n" +
                "Hello " + recipientName + ",\n\n" +
                "Here is your monthly reconciled financial overview:\n\n" +
                "Total Income: " + formatInr(totalIncome) + "\n" +
                "Total Expenses: " + formatInr(totalExpenses) + "\n" +
                "Net Cash Flow: " + formatInr(netCashFlow) + "\n" +
                "Savings Rate: " + String.format("%.1f%%", rate) + "\n\n" +
                "Category Spending:\n" + catText.toString() + "\n" +
                "View full reports at: " + appUrl + "/reports\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 8. Security Alert Template
    // ==========================================
    public EmailContent buildSecurityAlert(String recipientName, String alertTitle, String alertDescription, String actionRequired) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeTitle = escapeHtml(alertTitle);
        String safeDesc = escapeHtml(alertDescription);
        String safeAction = escapeHtml(actionRequired);

        String subject = "🔒 [FinTrack Security] " + safeTitle;

        String html = wrapHtml(
                "Security Alert",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #FEF2F2; color: #EF4444; border: 1px solid #FECACA; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">SECURITY ALERT</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 22px; font-weight: 800; margin: 16px 0 8px;\">" + safeTitle + "</h2>" +
                        "</div>" +
                        "<div style=\"background-color: #FEF2F2; border: 1px solid #FECACA; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <p style=\"color: #991B1B; font-size: 15px; line-height: 1.6; margin: 0 0 12px 0;\">Hello " + safeName + ",</p>" +
                        "  <p style=\"color: #7F1D1D; font-size: 14px; line-height: 1.6; margin: 0 0 12px 0;\">" + safeDesc + "</p>" +
                        "  <p style=\"color: #991B1B; font-size: 14px; font-weight: 700; margin: 0;\">Recommended Action: " + safeAction + "</p>" +
                        "</div>" +
                        renderButton("Review Account Security", appUrl + "/profile")
        );

        String text = "FINTRACK SECURITY ALERT\n\n" +
                "Hello " + recipientName + ",\n\n" +
                alertTitle + "\n\n" +
                alertDescription + "\n\n" +
                "Action: " + actionRequired + "\n\n" +
                "Account security: " + appUrl + "/profile\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // 9. Test Email Template (Development Verification)
    // ==========================================
    public EmailContent buildTestEmail(String recipientName, String userEmail, String customNote) {
        String safeName = escapeHtml(recipientName != null ? recipientName : "FinTrack User");
        String safeEmail = escapeHtml(userEmail);
        String safeNote = customNote != null && !customNote.isBlank() ? escapeHtml(customNote) : "No custom parameters supplied";
        String timestamp = LocalDate.now().format(DATE_FORMATTER);

        String subject = "✅ [FinTrack] Gmail SMTP Integration Test Verified";

        String html = wrapHtml(
                "SMTP Connectivity Test",
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                        "  <span style=\"background-color: #ECFDF5; color: #059669; border: 1px solid #A7F3D0; padding: 5px 14px; border-radius: 9999px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;\">SMTP SYSTEM VERIFIED</span>" +
                        "  <h2 style=\"color: #0F172A; font-size: 22px; font-weight: 800; margin: 16px 0 8px;\">Gmail SMTP Integration Live!</h2>" +
                        "  <p style=\"color: #475569; font-size: 15px; margin: 0;\">Hello " + safeName + ", your Gmail SMTP notification pipeline is operational.</p>" +
                        "</div>" +
                        "<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">" +
                        "  <table style=\"width: 100%; border-collapse: collapse;\">" +
                        renderRow("Verified Recipient", safeEmail) +
                        renderRow("SMTP Server", "smtp.gmail.com:587 (STARTTLS)") +
                        renderRow("Test Date", timestamp) +
                        renderRow("Integration Status", "<strong style=\"color: #10B981;\">CONNECTED &amp; ACCEPTED</strong>") +
                        renderRow("Note", safeNote) +
                        "  </table>" +
                        "</div>" +
                        "<p style=\"color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 24px;\">" +
                        "This diagnostic email was dispatched from the FinTrack transaction service backend via Google's STARTTLS relay. Delivery tracking and rate limit mechanisms are functioning normally." +
                        "</p>" +
                        renderButton("Return to Dashboard", appUrl + "/dashboard")
        );

        String text = "FINTRACK GMAIL SMTP INTEGRATION TEST\n\n" +
                "Hello " + recipientName + ",\n\n" +
                "Your Gmail SMTP integration test has been successfully verified!\n\n" +
                "Recipient: " + userEmail + "\n" +
                "Server: smtp.gmail.com:587 (STARTTLS)\n" +
                "Date: " + timestamp + "\n" +
                "Status: CONNECTED & ACCEPTED\n" +
                "Note: " + safeNote + "\n\n" +
                "FinTrack dashboard: " + appUrl + "/dashboard\n";

        return new EmailContent(subject, html, text);
    }

    // ==========================================
    // Internal HTML Layout Helpers (Bulletproof Fintech Email Architecture)
    // ==========================================
    private String renderRow(String label, String value) {
        return "<tr style=\"border-bottom: 1px solid #F1F5F9;\">" +
                "  <td style=\"padding: 12px 0; color: #64748B; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px;\">" + label + "</td>" +
                "  <td style=\"padding: 12px 0; color: #0F172A; font-size: 14px; text-align: right; font-weight: 600;\">" + value + "</td>" +
                "</tr>";
    }

    private String renderButton(String label, String url) {
        return "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 28px auto 8px;\" align=\"center\">" +
                "  <tr>" +
                "    <td align=\"center\" bgcolor=\"#10B981\" style=\"border-radius: 8px; box-shadow: 0 4px 14px rgba(16, 185, 129, 0.35);\">" +
                "      <a href=\"" + escapeHtml(url) + "\" target=\"_blank\" style=\"display: inline-block; padding: 13px 32px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 14px; color: #FFFFFF; font-weight: 700; text-decoration: none; border-radius: 8px; letter-spacing: 0.2px;\">" +
                escapeHtml(label) + " &rarr;" +
                "      </a>" +
                "    </td>" +
                "  </tr>" +
                "</table>";
    }

    private String wrapHtml(String preheader, String bodyContent) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "  <title>FinTrack Notification</title>\n" +
                "  <!--[if mso]><style>table,td,th{font-family:Arial,sans-serif!important;}</style><![endif]-->\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #F4F7F6; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; color: #0F172A;\">\n" +
                "  <!-- Preheader -->\n" +
                "  <div style=\"display: none; font-size: 1px; color: #FFFFFF; line-height: 1px; max-height: 0px; max-width: 0px; opacity: 0; overflow: hidden;\">\n" +
                "    " + escapeHtml(preheader) + "\n" +
                "  </div>\n" +
                "  \n" +
                "  <!-- Outer Container -->\n" +
                "  <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #F4F7F6; min-width: 100%;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\" style=\"padding: 36px 16px 48px;\">\n" +
                "        \n" +
                "        <!-- Main Card (Max 600px) -->\n" +
                "        <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; background-color: #FFFFFF; border-radius: 16px; border: 1px solid #E2E8F0; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04);\">\n" +
                "          \n" +
                "          <!-- Header Bar with Wordmark -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(170deg, #090E17 0%, #0F172A 60%, #0D1B2A 100%); padding: 24px 32px; border-bottom: 2px solid #10B981;\">\n" +
                "              <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                <tr>\n" +
                "                  <td align=\"left\" valign=\"middle\">\n" +
                "                    <span style=\"display: inline-block; width: 32px; height: 32px; background: linear-gradient(135deg, #10B981 0%, #059669 100%); border-radius: 8px; text-align: center; line-height: 32px; color: #FFFFFF; font-weight: 800; font-size: 16px; margin-right: 10px; vertical-align: middle; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);\">&#9650;</span>\n" +
                "                    <span style=\"color: #FFFFFF; font-size: 20px; font-weight: 800; letter-spacing: -0.5px; vertical-align: middle;\">Fin<span style=\"color: #10B981;\">Track</span></span>\n" +
                "                    <span style=\"color: #94A3B8; font-size: 11px; font-weight: 600; margin-left: 8px; text-transform: uppercase; letter-spacing: 0.5px; vertical-align: middle;\">Enterprise Systems</span>\n" +
                "                  </td>\n" +
                "                  <td align=\"right\" valign=\"middle\">\n" +
                "                    <span style=\"background-color: rgba(16, 185, 129, 0.15); color: #10B981; border: 1px solid rgba(16, 185, 129, 0.35); font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 9999px; text-transform: uppercase; letter-spacing: 0.5px;\">Live Engine</span>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          \n" +
                "          <!-- Body Area -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 32px 32px 28px;\">\n" +
                "              " + bodyContent + "\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          \n" +
                "          <!-- Footer Area -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #F8FAFC; padding: 24px 32px; border-top: 1px solid #E2E8F0; text-align: center;\">\n" +
                "              <p style=\"color: #64748B; font-size: 12px; line-height: 1.5; margin: 0 0 6px 0;\">\n" +
                "                You received this transmission because notification alerts are enabled in your FinTrack profile.\n" +
                "              </p>\n" +
                "              <p style=\"color: #94A3B8; font-size: 12px; line-height: 1.5; margin: 0 0 10px 0;\">\n" +
                "                Manage preferences: <a href=\"" + escapeHtml(appUrl) + "/settings\" style=\"color: #059669; font-weight: 600; text-decoration: underline;\">Account Notification Settings</a>\n" +
                "              </p>\n" +
                "              <p style=\"color: #CBD5E1; font-size: 11px; line-height: 1.4; margin: 0;\">\n" +
                "                &copy; " + LocalDate.now().getYear() + " FinTrack Financial Analytics &amp; Budget Reconciliation System. Reconciled securely.\n" +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          \n" +
                "        </table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }
}
