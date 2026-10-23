package com.factory.expenses.service;

import com.factory.expenses.api.dto.CategoryTotal;
import com.factory.expenses.api.dto.MonthlySummaryResponse;
import com.factory.expenses.config.AppProperties;
import com.factory.expenses.repository.CategoryAggregate;
import com.factory.expenses.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

  private static final Comparator<CategoryTotal> ORDER =
      Comparator.comparing(CategoryTotal::total).reversed().thenComparing(CategoryTotal::category);

  private final ExpenseRepository repository;
  private final AppProperties properties;

  public SummaryService(ExpenseRepository repository, AppProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public MonthlySummaryResponse monthly(YearMonth month) {
    List<CategoryAggregate> rows =
        repository.totalsByCategory(month.atDay(1), month.atEndOfMonth());
    List<CategoryTotal> categories =
        rows.stream()
            .map(r -> new CategoryTotal(r.category(), r.total().setScale(2), r.count()))
            .sorted(ORDER)
            .toList();
    BigDecimal total =
        categories.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    long count = categories.stream().mapToLong(CategoryTotal::count).sum();
    return new MonthlySummaryResponse(
        month.toString(), properties.currency(), total.setScale(2), count, categories);
  }
}
