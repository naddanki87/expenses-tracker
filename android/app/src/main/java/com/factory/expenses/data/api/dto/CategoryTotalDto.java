package com.factory.expenses.data.api.dto;

import java.math.BigDecimal;

/** Total of one category inside a monthly summary. */
public final class CategoryTotalDto {
  public final String category;
  public final BigDecimal total;
  public final long count;

  public CategoryTotalDto(String category, BigDecimal total, long count) {
    this.category = category;
    this.total = total;
    this.count = count;
  }
}
