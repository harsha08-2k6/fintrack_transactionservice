package com.spendwise.transaction.repository;

import com.spendwise.transaction.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<NotificationDelivery> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationDelivery> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndNotificationTypeAndCreatedAtAfter(Long userId, String notificationType, LocalDateTime after);
}
