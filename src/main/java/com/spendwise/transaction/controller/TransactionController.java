package com.spendwise.transaction.controller;

import com.spendwise.transaction.dto.TransactionRequest;
import com.spendwise.transaction.dto.TransactionResponse;
import com.spendwise.transaction.entity.TransactionType;
import com.spendwise.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ==========================================
    // Unified Transaction Endpoints: /api/transactions
    // ==========================================

    @PostMapping("/api/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionResponse> list = transactionService.getTransactions(userId, type, categoryId, startDate, endDate);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable("id") Long id) {
        TransactionResponse response = transactionService.getTransactionById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable("id") Long id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable("id") Long id) {
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Income Specific Endpoints: /api/income & /api/transactions/income
    // ==========================================

    @GetMapping({"/api/income", "/api/transactions/income"})
    public ResponseEntity<List<TransactionResponse>> getIncome(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionResponse> list = transactionService.getTransactions(userId, TransactionType.INCOME, null, startDate, endDate);
        return ResponseEntity.ok(list);
    }

    @PostMapping({"/api/income", "/api/transactions/income"})
    public ResponseEntity<TransactionResponse> createIncome(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody TransactionRequest request) {
        request.setType(TransactionType.INCOME);
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==========================================
    // Expense Specific Endpoints: /api/expenses & /api/transactions/expenses
    // ==========================================

    @GetMapping({"/api/expenses", "/api/transactions/expenses"})
    public ResponseEntity<List<TransactionResponse>> getExpenses(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionResponse> list = transactionService.getTransactions(userId, TransactionType.EXPENSE, categoryId, startDate, endDate);
        return ResponseEntity.ok(list);
    }

    @PostMapping({"/api/expenses", "/api/transactions/expenses"})
    public ResponseEntity<TransactionResponse> createExpense(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody TransactionRequest request) {
        request.setType(TransactionType.EXPENSE);
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping({"/api/expenses/{id}"})
    public ResponseEntity<TransactionResponse> getExpenseById(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable("id") Long id) {
        TransactionResponse response = transactionService.getTransactionById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/api/expenses/{id}"})
    public ResponseEntity<TransactionResponse> updateExpense(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable("id") Long id,
            @Valid @RequestBody TransactionRequest request) {
        request.setType(TransactionType.EXPENSE);
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/api/expenses/{id}"})
    public ResponseEntity<Void> deleteExpense(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable("id") Long id) {
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }
}
