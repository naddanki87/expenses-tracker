package com.factory.expenses.api.dto;

import com.factory.expenses.domain.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
    @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @DecimalMax("9999999999.99")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,
    @NotNull Category category,
    @NotNull @NotInFuture LocalDate date,
    @Size(max = 500) String note) {

  /** Trims the note; a blank note becomes null. */
  public ExpenseRequest {
    note = note == null || note.isBlank() ? null : note.strip();
  }
}
