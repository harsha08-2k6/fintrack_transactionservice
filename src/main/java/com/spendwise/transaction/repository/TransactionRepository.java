package com.spendwise.transaction.repository;

import com.spendwise.transaction.entity.Transaction;
import com.spendwise.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdAndTypeOrderByTransactionDateDescCreatedAtDesc(Long userId, TransactionType type);

    List<Transaction> findByUserIdAndCategoryIdOrderByTransactionDateDesc(Long userId, Long categoryId);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, TransactionType type, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.categoryId, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = :type " +
           "GROUP BY t.categoryId")
    List<Object[]> getCategoryWiseSums(@Param("userId") Long userId, @Param("type") TransactionType type);
}
