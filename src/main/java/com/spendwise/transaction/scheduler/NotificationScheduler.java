package com.spendwise.transaction.scheduler;

import com.spendwise.transaction.entity.NotificationPreference;
import com.spendwise.transaction.repository.NotificationPreferenceRepository;
import com.spendwise.transaction.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final NotificationPreferenceRepository preferenceRepository;

    @Value("${fintrack.scheduler.monthly-summary-enabled:true}")
    private boolean monthlySummaryScheduled;

    public NotificationScheduler(
            NotificationService notificationService,
            NotificationPreferenceRepository preferenceRepository) {
        this.notificationService = notificationService;
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Reconciles and dispatches monthly financial summary statements for the preceding calendar month.
     * Fires at 09:00 AM on the 1st of every month by default.
     */
    @Scheduled(cron = "${fintrack.scheduler.monthly-summary-cron:0 0 9 1 * ?}")
    public void runMonthlyFinancialSummaryJob() {
        if (!monthlySummaryScheduled) {
            log.info("Scheduled monthly summary job is disabled via configuration");
            return;
        }

        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("Running scheduled monthly financial summary dispatch job for period: {}", previousMonth);

        try {
            List<NotificationPreference> optInPrefs = preferenceRepository.findAll().stream()
                    .filter(p -> p.isEmailNotificationsEnabled() && p.isMonthlySummaryEnabled())
                    .toList();

            for (NotificationPreference pref : optInPrefs) {
                try {
                    notificationService.processMonthlyFinancialSummary(pref.getUserId(), previousMonth);
                } catch (Exception e) {
                    log.error("Failed to generate monthly summary for userId={}: {}", pref.getUserId(), e.getMessage());
                }
            }
            log.info("Scheduled monthly financial summary dispatch completed for {} opted-in users", optInPrefs.size());
        } catch (Exception e) {
            log.error("Error executing monthly summary scheduled task: {}", e.getMessage(), e);
        }
    }
}
