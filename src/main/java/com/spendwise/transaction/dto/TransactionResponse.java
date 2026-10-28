package com.spendwise.transaction.dto;

import com.spendwise.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private Long userId;
    private Long categoryId;
    private CategoryDto category;
    private String title;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private String paymentMethod;
    private LocalDate transactionDate;
    private LocalDate expenseDate; // Backward compatibility alias
    private String receiptUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TransactionResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public CategoryDto getCategory() { return category; }
    public void setCategory(CategoryDto category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { 
        this.transactionDate = transactionDate; 
        this.expenseDate = transactionDate;
    }

    public LocalDate getExpenseDate() { return transactionDate; }
    public void setExpenseDate(LocalDate expenseDate) { 
        this.expenseDate = expenseDate; 
        if (this.transactionDate == null) this.transactionDate = expenseDate;
    }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
