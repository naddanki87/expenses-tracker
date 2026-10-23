package com.factory.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.factory.expenses.api.dto.ExpenseRequest;
import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import com.factory.expenses.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-21T12:34:56Z");

  @Mock private ExpenseRepository repository;

  private ExpenseService service;

  @BeforeEach
  void setUp() {
    service = new ExpenseService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static ExpenseRequest request(String amount) {
    return new ExpenseRequest(
        new BigDecimal(amount), Category.FOOD, LocalDate.of(2026, 10, 21), "  lunch ");
  }

  @Test
  void createStoresAmountWithScaleTwoAndTimestamps() {
    when(repository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

    Expense created = service.create(request("45"));

    assertThat(created.getAmount()).isEqualTo(new BigDecimal("45.00"));
    assertThat(created.getAmount().scale()).isEqualTo(2);
    assertThat(created.getNote()).isEqualTo("lunch");
    assertThat(created.getCreatedAt()).isEqualTo(NOW).isEqualTo(created.getUpdatedAt());
  }

  @Test
  void getReturnsExpense() {
    Expense expense = new Expense(BigDecimal.TEN, Category.OTHER, LocalDate.now(), null, NOW);
    when(repository.findById(7L)).thenReturn(Optional.of(expense));

    assertThat(service.get(7L)).isSameAs(expense);
  }

  @Test
  void getUnknownThrowsNotFound() {
    when(repository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(9L))
        .isInstanceOf(ExpenseNotFoundException.class)
        .hasMessage("Expense 9 not found")
        .extracting(e -> ((ExpenseNotFoundException) e).getExpenseId())
        .isEqualTo(9L);
  }

  @Test
  void updateReplacesFieldsKeepsCreatedAtAndRefreshesUpdatedAt() {
    Instant earlier = NOW.minusSeconds(3600);
    Expense existing =
        new Expense(BigDecimal.ONE, Category.OTHER, LocalDate.of(2026, 1, 1), "old", earlier);
    when(repository.findById(3L)).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);

    Expense updated = service.update(3L, request("14.5"));

    assertThat(updated.getAmount()).isEqualTo(new BigDecimal("14.50"));
    assertThat(updated.getCategory()).isEqualTo(Category.FOOD);
    assertThat(updated.getDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    assertThat(updated.getNote()).isEqualTo("lunch");
    assertThat(updated.getCreatedAt()).isEqualTo(earlier);
    assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void updateUnknownThrowsNotFound() {
    when(repository.findById(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(3L, request("1")))
        .isInstanceOf(ExpenseNotFoundException.class);
  }

  @Test
  void deleteRemovesExisting() {
    Expense existing = new Expense(BigDecimal.ONE, Category.OTHER, LocalDate.now(), null, NOW);
    when(repository.findById(3L)).thenReturn(Optional.of(existing));

    service.delete(3L);

    verify(repository).delete(existing);
  }

  @Test
  void deleteUnknownThrowsNotFound() {
    when(repository.findById(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ExpenseNotFoundException.class);
  }

  @Test
  void listPassesMonthBoundsAndCategory() {
    when(repository.search(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), Category.HEALTH))
        .thenReturn(List.of());

    List<Expense> result =
        service.list(Optional.of(YearMonth.of(2026, Month.FEBRUARY)), Optional.of(Category.HEALTH));

    assertThat(result).isEmpty();
  }

  @Test
  void listWithoutFiltersPassesNulls() {
    when(repository.search(null, null, null)).thenReturn(List.of());

    assertThat(service.list(Optional.empty(), Optional.empty())).isEmpty();
  }
}
