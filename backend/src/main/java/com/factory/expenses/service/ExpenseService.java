package com.factory.expenses.service;

import com.factory.expenses.api.dto.ExpenseRequest;
import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import com.factory.expenses.repository.ExpenseRepository;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

  private final ExpenseRepository repository;
  private final Clock clock;

  public ExpenseService(ExpenseRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public Expense create(ExpenseRequest request) {
    Expense expense =
        new Expense(
            request.amount().setScale(2, RoundingMode.UNNECESSARY),
            request.category(),
            request.date(),
            request.note(),
            now());
    return repository.save(expense);
  }

  @Transactional(readOnly = true)
  public Expense get(long id) {
    return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
  }

  @Transactional
  public Expense update(long id, ExpenseRequest request) {
    Expense expense = get(id);
    expense.replaceWith(
        request.amount().setScale(2, RoundingMode.UNNECESSARY),
        request.category(),
        request.date(),
        request.note(),
        now());
    return repository.save(expense);
  }

  @Transactional
  public void delete(long id) {
    repository.delete(get(id));
  }

  @Transactional(readOnly = true)
  public List<Expense> list(Optional<YearMonth> month, Optional<Category> category) {
    return repository.search(
        month.map(m -> m.atDay(1)).orElse(null),
        month.map(YearMonth::atEndOfMonth).orElse(null),
        category.orElse(null));
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MILLIS);
  }
}
