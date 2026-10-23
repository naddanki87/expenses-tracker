package com.factory.expenses.repository;

import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

  @Query(
      """
      select e from Expense e
      where (cast(:from as localdate) is null or e.date >= :from)
        and (cast(:to as localdate) is null or e.date <= :to)
        and (:category is null or e.category = :category)
      order by e.date desc, e.id desc
      """)
  List<Expense> search(
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("category") Category category);

  @Query(
      """
      select new com.factory.expenses.repository.CategoryAggregate(
          e.category, sum(e.amount), count(e))
      from Expense e
      where e.date >= :from and e.date <= :to
      group by e.category
      """)
  List<CategoryAggregate> totalsByCategory(
      @Param("from") LocalDate from, @Param("to") LocalDate to);
}
