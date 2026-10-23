package com.factory.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.factory.expenses.api.dto.CategoryTotal;
import com.factory.expenses.api.dto.MonthlySummaryResponse;
import com.factory.expenses.config.AppProperties;
import com.factory.expenses.domain.Category;
import com.factory.expenses.repository.CategoryAggregate;
import com.factory.expenses.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

  private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

  @Mock private ExpenseRepository repository;

  private SummaryService service;

  @BeforeEach
  void setUp() {
    service = new SummaryService(repository, new AppProperties("EUR"));
  }

  private void rows(CategoryAggregate... rows) {
    when(repository.totalsByCategory(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
        .thenReturn(List.of(rows));
  }

  @Test
  void ordersByTotalDescendingAndSumsTotals() {
    rows(
        new CategoryAggregate(Category.TRANSPORT, new BigDecimal("35.00"), 1),
        new CategoryAggregate(Category.FOOD, new BigDecimal("60.50"), 2),
        new CategoryAggregate(Category.HOUSING, new BigDecimal("150.00"), 1));

    MonthlySummaryResponse summary = service.monthly(OCTOBER);

    assertThat(summary.month()).isEqualTo("2026-10");
    assertThat(summary.currency()).isEqualTo("EUR");
    assertThat(summary.total()).isEqualTo(new BigDecimal("245.50"));
    assertThat(summary.count()).isEqualTo(4);
    assertThat(summary.categories())
        .extracting(CategoryTotal::category)
        .containsExactly(Category.HOUSING, Category.FOOD, Category.TRANSPORT);
  }

  @Test
  void equalTotalsAreOrderedByCategoryCode() {
    rows(
        new CategoryAggregate(Category.TRAVEL, new BigDecimal("10.00"), 1),
        new CategoryAggregate(Category.FOOD, new BigDecimal("10.00"), 1),
        new CategoryAggregate(Category.HEALTH, new BigDecimal("10.00"), 1));

    assertThat(service.monthly(OCTOBER).categories())
        .extracting(CategoryTotal::category)
        .containsExactly(Category.FOOD, Category.HEALTH, Category.TRAVEL);
  }

  @Test
  void emptyMonthHasZeroTotalWithScaleTwo() {
    rows();

    MonthlySummaryResponse summary = service.monthly(OCTOBER);

    assertThat(summary.total()).isEqualTo(new BigDecimal("0.00"));
    assertThat(summary.total().scale()).isEqualTo(2);
    assertThat(summary.count()).isZero();
    assertThat(summary.categories()).isEmpty();
  }
}
