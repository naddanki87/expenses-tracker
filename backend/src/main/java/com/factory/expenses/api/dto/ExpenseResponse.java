package com.factory.expenses.api.dto;

import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
    Long id,
    BigDecimal amount,
    Category category,
    LocalDate date,
    String note,
    Instant createdAt,
    Instant updatedAt) {

  public static ExpenseResponse from(Expense expense) {
    return new ExpenseResponse(
        expense.getId(),
        expense.getAmount(),
        expense.getCategory(),
        expense.getDate(),
        expense.getNote(),
        expense.getCreatedAt(),
        expense.getUpdatedAt());
  }
}
