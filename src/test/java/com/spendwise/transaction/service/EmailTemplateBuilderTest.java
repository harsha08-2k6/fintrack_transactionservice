package com.spendwise.transaction.service;

import com.spendwise.transaction.dto.CategorySummaryItem;
import com.spendwise.transaction.dto.EmailContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateBuilderTest {

    private EmailTemplateBuilder templateBuilder;

    @BeforeEach
    void setUp() {
        templateBuilder = new EmailTemplateBuilder();
        ReflectionTestUtils.setField(templateBuilder, "appUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("EscapeHtml should sanitize dangerous HTML characters to prevent XSS")
    void testEscapeHtml() {
        String unsafe = "<script>alert('xss');</script>&\"test\"";
        String safe = templateBuilder.escapeHtml(unsafe);

        assertFalse(safe.contains("<script>"));
        assertTrue(safe.contains("&lt;script&gt;"));
        assertTrue(safe.contains("&amp;"));
        assertTrue(safe.contains("&quot;"));
        assertTrue(safe.contains("&#39;"));
    }

    @Test
    @DisplayName("FormatInr should format amounts correctly with Indian Rupee symbol")
    void testFormatInr() {
        String formatted = templateBuilder.formatInr(new BigDecimal("15450.50"));
        assertNotNull(formatted);
        assertTrue(formatted.contains("₹") || formatted.contains("15,450"));
    }

    @Test
    @DisplayName("BuildBudgetAlert renders 80% warning correctly")
    void testBuildBudgetAlert80Percent() {
        EmailContent content = templateBuilder.buildBudgetAlert(
                "Harsha", "Dining Out",
                new BigDecimal("8500.00"), new BigDecimal("10000.00"),
                new BigDecimal("1500.00"), "2026-10", "WARNING");

        assertNotNull(content);
        assertTrue(content.getSubject().contains("Dining Out"));
        assertTrue(content.getHtmlBody().contains("Dining Out"));
        assertTrue(content.getHtmlBody().contains("80%"));
        assertTrue(content.getHtmlBody().contains("http://localhost:5173/categories"));
        assertNotNull(content.getTextBody());
        assertTrue(content.getTextBody().contains("Dining Out"));
    }

    @Test
    @DisplayName("BuildBudgetAlert renders over-budget critical alert correctly")
    void testBuildBudgetAlertOverspend() {
        EmailContent content = templateBuilder.buildBudgetAlert(
                "Harsha", "Shopping",
                new BigDecimal("12000.00"), new BigDecimal("10000.00"),
                new BigDecimal("-2000.00"), "2026-10", "CRITICAL");

        assertTrue(content.getSubject().contains("Exceeded"));
        assertTrue(content.getHtmlBody().contains("Budget Limit Exceeded: Shopping"));
        assertTrue(content.getHtmlBody().contains("CRITICAL"));
    }

    @Test
    @DisplayName("BuildExpenseConfirmation contains transaction details and plain text fallback")
    void testBuildExpenseConfirmation() {
        EmailContent content = templateBuilder.buildExpenseConfirmation(
                "Harsha", "AWS Cloud Hosting", new BigDecimal("4500.00"),
                "Cloud Services", LocalDate.of(2026, 10, 24), "CREDIT_CARD",
                "Monthly infrastructure invoice");

        assertTrue(content.getSubject().contains("AWS Cloud Hosting"));
        assertTrue(content.getHtmlBody().contains("AWS Cloud Hosting"));
        assertTrue(content.getHtmlBody().contains("CREDIT_CARD"));
        assertTrue(content.getTextBody().contains("Cloud Services"));
    }

    @Test
    @DisplayName("BuildIncomeNotification contains inflow details")
    void testBuildIncomeNotification() {
        EmailContent content = templateBuilder.buildIncomeNotification(
                "Harsha", "Software Engineering Salary", new BigDecimal("125000.00"),
                LocalDate.of(2026, 10, 1), "DIRECT_DEPOSIT");

        assertTrue(content.getSubject().contains("Software Engineering Salary"));
        assertTrue(content.getHtmlBody().contains("INCOME INFLOW"));
        assertTrue(content.getTextBody().contains("DIRECT_DEPOSIT"));
    }

    @Test
    @DisplayName("BuildSavingsGoalMilestone renders celebration and progress")
    void testBuildSavingsGoalMilestone() {
        EmailContent content = templateBuilder.buildSavingsGoalMilestone(
                "Harsha", "Emergency Fund", 50,
                new BigDecimal("50000.00"), new BigDecimal("100000.00"));

        assertTrue(content.getSubject().contains("50%"));
        assertTrue(content.getHtmlBody().contains("Emergency Fund"));
        assertTrue(content.getTextBody().contains("Emergency Fund"));
    }

    @Test
    @DisplayName("BuildMonthlySummary renders totals and categories")
    void testBuildMonthlySummary() {
        List<CategorySummaryItem> categories = List.of(
                new CategorySummaryItem("Food", new BigDecimal("8000"), 40.0),
                new CategorySummaryItem("Rent", new BigDecimal("12000"), 60.0)
        );

        EmailContent content = templateBuilder.buildMonthlySummary(
                "Harsha", "2026-10",
                new BigDecimal("50000.00"), new BigDecimal("20000.00"),
                new BigDecimal("30000.00"), 60.0, categories);

        assertTrue(content.getSubject().contains("2026-10"));
        assertTrue(content.getHtmlBody().contains("Food"));
        assertTrue(content.getHtmlBody().contains("Rent"));
        assertTrue(content.getTextBody().contains("Net Cash Flow"));
    }

    @Test
    @DisplayName("BuildTestEmail contains verification markers and recipient")
    void testBuildTestEmail() {
        EmailContent content = templateBuilder.buildTestEmail(
                "Harsha", "harsha@example.com", "Manual verification test");

        assertTrue(content.getSubject().contains("SMTP Integration Test Verified"));
        assertTrue(content.getHtmlBody().contains("harsha@example.com"));
        assertTrue(content.getHtmlBody().contains("smtp.gmail.com:587"));
        assertTrue(content.getTextBody().contains("CONNECTED & ACCEPTED"));
    }

    @Test
    @DisplayName("BuildSecurityAlert contains title, warning alert, and action required")
    void testBuildSecurityAlert() {
        EmailContent content = templateBuilder.buildSecurityAlert(
                "Harsha", "Unusual Account Access Detected",
                "A login from a new device was recorded.",
                "Review active sessions immediately.");

        assertTrue(content.getSubject().contains("Security"));
        assertTrue(content.getHtmlBody().contains("SECURITY ALERT"));
        assertTrue(content.getHtmlBody().contains("Unusual Account Access Detected"));
        assertTrue(content.getTextBody().contains("Review active sessions immediately"));
    }
}
