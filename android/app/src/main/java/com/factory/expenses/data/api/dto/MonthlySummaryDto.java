package com.factory.expenses.data.api.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Monthly total and per-category totals. */
public final class MonthlySummaryDto {
  public final YearMonth month;
  public final String currency;
  public final BigDecimal total;
  public final long count;
  public final List<CategoryTotalDto> categories;

  public MonthlySummaryDto(
      YearMonth month,
      String currency,
      BigDecimal total,
      long count,
      List<CategoryTotalDto> categories) {
    this.month = month;
    this.currency = currency;
    this.total = total;
    this.count = count;
    this.categories = categories;
  }
}
