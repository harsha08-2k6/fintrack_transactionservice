package com.spendwise.transaction.service.impl;

import com.spendwise.transaction.client.CategoryClient;
import com.spendwise.transaction.dto.CategoryDto;
import com.spendwise.transaction.dto.TransactionRequest;
import com.spendwise.transaction.dto.TransactionResponse;
import com.spendwise.transaction.entity.Transaction;
import com.spendwise.transaction.entity.TransactionType;
import com.spendwise.transaction.repository.TransactionRepository;
import com.spendwise.transaction.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final CategoryClient categoryClient;
    private final com.spendwise.transaction.service.NotificationService notificationService;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            CategoryClient categoryClient,
            com.spendwise.transaction.service.NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.categoryClient = categoryClient;
        this.notificationService = notificationService;
    }

    @Override
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setCategoryId(request.getCategoryId());
        transaction.setTitle(request.getTitle());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType() != null ? request.getType() : TransactionType.EXPENSE);
        transaction.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARD");
        transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now());
        transaction.setReceiptUrl(request.getReceiptUrl());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, type={}, amount={}, userId={}", saved.getId(), saved.getType(), saved.getAmount(), userId);

        // Event integration: Trigger asynchronous notification & email dispatch (non-blocking)
        try {
            if (saved.getType() == TransactionType.EXPENSE) {
                notificationService.processExpenseCreated(saved);
            } else if (saved.getType() == TransactionType.INCOME) {
                notificationService.processIncomeCreated(saved);
            }
        } catch (Exception e) {
            log.warn("Non-fatal notification dispatch error for transactionId={}: {}", saved.getId(), e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }

        return mapToResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId, TransactionType type, Long categoryId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions;

        if (type != null && startDate != null && endDate != null) {
            transactions = transactionRepository.findByUserIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(userId, type, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate);
        } else if (type != null) {
            transactions = transactionRepository.findByUserIdAndTypeOrderByTransactionDateDescCreatedAtDesc(userId, type);
        } else if (categoryId != null) {
            transactions = transactionRepository.findByUserIdAndCategoryIdOrderByTransactionDateDesc(userId, categoryId);
        } else {
            transactions = transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId);
        }

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }

        transaction.setTitle(request.getTitle());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        transaction.setCategoryId(request.getCategoryId());
        if (request.getPaymentMethod() != null) {
            transaction.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }
        if (request.getReceiptUrl() != null) {
            transaction.setReceiptUrl(request.getReceiptUrl());
        }

        Transaction updated = transactionRepository.save(transaction);
        log.info("Transaction updated: id={}, userId={}", updated.getId(), userId);
        return mapToResponse(updated);
    }

    @Override
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }

        transactionRepository.delete(transaction);
        log.info("Transaction deleted: id={}, userId={}", transactionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalAmountByType(Long userId, TransactionType type) {
        return transactionRepository.sumAmountByUserIdAndType(userId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyAmountByType(Long userId, TransactionType type, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, type, start, end);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        TransactionResponse res = new TransactionResponse();
        res.setId(t.getId());
        res.setUserId(t.getUserId());
        res.setCategoryId(t.getCategoryId());
        res.setTitle(t.getTitle());
        res.setDescription(t.getDescription());
        res.setAmount(t.getAmount());
        res.setType(t.getType());
        res.setPaymentMethod(t.getPaymentMethod());
        res.setTransactionDate(t.getTransactionDate());
        res.setReceiptUrl(t.getReceiptUrl());
        res.setCreatedAt(t.getCreatedAt());
        res.setUpdatedAt(t.getUpdatedAt());

        // Fetch category details if categoryId is present
        if (t.getCategoryId() != null && t.getCategoryId() > 0) {
            try {
                CategoryDto category = categoryClient.getCategoryById(t.getCategoryId());
                res.setCategory(category);
            } catch (Exception e) {
                log.warn("Could not fetch category details for categoryId {}: {}", t.getCategoryId(), e.getMessage());
            }
        }

        return res;
    }
}
