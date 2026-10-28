package com.spendwise.transaction.service;

import com.spendwise.transaction.dto.TransactionRequest;
import com.spendwise.transaction.dto.TransactionResponse;
import com.spendwise.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(Long userId, TransactionRequest request);

    TransactionResponse getTransactionById(Long userId, Long transactionId);

    List<TransactionResponse> getTransactions(Long userId, TransactionType type, Long categoryId, LocalDate startDate, LocalDate endDate);

    TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request);

    void deleteTransaction(Long userId, Long transactionId);

    BigDecimal getTotalAmountByType(Long userId, TransactionType type);

    BigDecimal getMonthlyAmountByType(Long userId, TransactionType type, int year, int month);
}
