package com.factory.expenses.data.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of create and update calls. */
public final class ExpenseRequestDto {
  public final BigDecimal amount;
  public final String category;
  public final LocalDate date;
  public final String note;

  public ExpenseRequestDto(BigDecimal amount, String category, LocalDate date, String note) {
    this.amount = amount;
    this.category = category;
    this.date = date;
    this.note = note;
  }
}
