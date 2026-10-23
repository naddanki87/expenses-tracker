package com.factory.expenses.data.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Expense as returned by the API. Amounts are never floating point. */
public final class ExpenseDto {
  public final long id;
  public final BigDecimal amount;
  public final String category;
  public final LocalDate date;
  public final String note;
  public final Instant createdAt;
  public final Instant updatedAt;

  public ExpenseDto(
      long id,
      BigDecimal amount,
      String category,
      LocalDate date,
      String note,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.amount = amount;
    this.category = category;
    this.date = date;
    this.note = note;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
