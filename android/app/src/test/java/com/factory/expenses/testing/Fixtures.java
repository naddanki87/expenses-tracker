package com.factory.expenses.testing;

import com.factory.expenses.data.api.dto.CategoryInfoDto;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

/** Shared test data. */
public final class Fixtures {

  public static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

  private Fixtures() {}

  public static MetadataDto metadata() {
    return new MetadataDto(
        "USD",
        Arrays.asList(
            new CategoryInfoDto("FOOD", "Food"),
            new CategoryInfoDto("TRANSPORT", "Transport"),
            new CategoryInfoDto("OTHER", "Other")));
  }

  public static ExpenseDto expense(long id, String amount, String category, String date) {
    Instant now = Instant.parse("2026-10-21T12:34:56Z");
    return new ExpenseDto(
        id, new BigDecimal(amount), category, LocalDate.parse(date), "note", now, now);
  }
}
