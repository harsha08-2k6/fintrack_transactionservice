package com.spendwise.transaction.repository;

import com.spendwise.transaction.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, String> {

    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<InAppNotification> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);

    long countByUserIdAndReadFalse(Long userId);

    Optional<InAppNotification> findByIdAndUserId(String id, Long userId);

    void deleteByIdAndUserId(String id, Long userId);

    void deleteByUserId(Long userId);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsReadForUser(@Param("userId") Long userId);
}
