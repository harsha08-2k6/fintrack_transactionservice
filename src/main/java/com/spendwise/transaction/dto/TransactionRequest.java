package com.spendwise.transaction.dto;

import com.spendwise.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    private Long categoryId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private TransactionType type = TransactionType.EXPENSE;

    private String paymentMethod = "CARD";

    private LocalDate transactionDate;
    private LocalDate expenseDate; // Backward compatibility alias

    private String receiptUrl;

    public TransactionRequest() {}

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type != null ? type : TransactionType.EXPENSE; }
    public void setType(TransactionType type) { this.type = type; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getTransactionDate() { 
        return transactionDate != null ? transactionDate : (expenseDate != null ? expenseDate : LocalDate.now()); 
    }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public LocalDate getExpenseDate() { return getTransactionDate(); }
    public void setExpenseDate(LocalDate expenseDate) { 
        this.expenseDate = expenseDate; 
        if (this.transactionDate == null) this.transactionDate = expenseDate;
    }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
}
