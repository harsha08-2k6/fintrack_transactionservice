package com.spendwise.transaction.dto;

import java.math.BigDecimal;

public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private String colorCode;
    private String icon;
    private BigDecimal budgetLimit;

    public CategoryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public BigDecimal getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(BigDecimal budgetLimit) { this.budgetLimit = budgetLimit; }
}
