package com.factory.expenses.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.flyway.enabled=true")
class ExpenseRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

  @Autowired private ExpenseRepository repository;
  @Autowired private JdbcTemplate jdbc;

  private Expense save(String amount, Category category, String date) {
    return repository.saveAndFlush(
        new Expense(new BigDecimal(amount), category, LocalDate.parse(date), null, NOW));
  }

  @Test
  void savesAndReloadsWithScalePreserved() {
    Expense saved =
        repository.saveAndFlush(
            new Expense(
                new BigDecimal("12.50"), Category.FOOD, LocalDate.of(2026, 10, 21), "Lunch", NOW));
    repository.flush();

    Expense reloaded = jdbcReload(saved.getId());

    assertThat(reloaded.getAmount()).isEqualByComparingTo("12.50");
    assertThat(reloaded.getAmount().scale()).isEqualTo(2);
    assertThat(reloaded.getNote()).isEqualTo("Lunch");
    assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
  }

  private Expense jdbcReload(Long id) {
    return repository.findById(id).orElseThrow();
  }

  @Test
  void searchFiltersByRangeAndCategoryOrderedByDateThenIdDescending() {
    Expense sep30 = save("1.00", Category.FOOD, "2026-09-30");
    Expense oct1 = save("2.00", Category.FOOD, "2026-10-01");
    Expense oct15Food = save("3.00", Category.FOOD, "2026-10-15");
    Expense oct15Travel = save("4.00", Category.TRAVEL, "2026-10-15");
    Expense oct31 = save("5.00", Category.FOOD, "2026-10-31");
    Expense nov1 = save("6.00", Category.FOOD, "2026-11-01");
    LocalDate from = LocalDate.of(2026, 10, 1);
    LocalDate to = LocalDate.of(2026, 10, 31);

    assertThat(repository.search(from, to, null))
        .containsExactly(oct31, oct15Travel, oct15Food, oct1);
    assertThat(repository.search(from, to, Category.FOOD)).containsExactly(oct31, oct15Food, oct1);
    assertThat(repository.search(null, null, Category.FOOD))
        .containsExactly(nov1, oct31, oct15Food, oct1, sep30);
    assertThat(repository.search(null, null, null)).hasSize(6);
    assertThat(repository.search(from, to, Category.HEALTH)).isEmpty();
  }

  @Test
  void totalsByCategoryAggregatesInDatabaseWithinMonth() {
    save("12.50", Category.FOOD, "2026-10-03");
    save("48.00", Category.FOOD, "2026-10-31");
    save("150.00", Category.HOUSING, "2026-10-01");
    save("99.00", Category.FOOD, "2026-09-30");
    save("99.00", Category.FOOD, "2026-11-01");

    List<CategoryAggregate> rows =
        repository.totalsByCategory(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

    assertThat(rows)
        .extracting(
            CategoryAggregate::category, a -> a.total().toPlainString(), CategoryAggregate::count)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(Category.FOOD, "60.50", 2L),
            org.assertj.core.groups.Tuple.tuple(Category.HOUSING, "150.00", 1L));
  }

  @Test
  void databaseRejectsNonPositiveAmount() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into expense (amount, category, expense_date, created_at, updated_at)"
                        + " values (0, 'FOOD', DATE '2026-10-21', CURRENT_TIMESTAMP,"
                        + " CURRENT_TIMESTAMP)"))
        .hasMessageContaining("CK_EXPENSE_AMOUNT_POSITIVE");
  }
}
