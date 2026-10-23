package com.factory.expenses.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.ExpenseRequestDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import com.factory.expenses.data.api.dto.ProblemDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.Test;

/** The example payloads of docs/openapi.yaml must deserialize exactly. */
public class JsonContractTest {

  private final Gson gson = JsonSupport.gson();

  @Test
  public void expenseListExampleKeepsDecimalScaleAndNullNote() {
    String json =
        "[{\"id\":42,\"amount\":12.50,\"category\":\"FOOD\",\"date\":\"2026-10-21\","
            + "\"note\":\"Lunch with team\",\"createdAt\":\"2026-10-21T12:34:56Z\","
            + "\"updatedAt\":\"2026-10-21T12:34:56Z\"},"
            + "{\"id\":37,\"amount\":8.00,\"category\":\"FOOD\",\"date\":\"2026-10-03\","
            + "\"note\":null,\"createdAt\":\"2026-10-03T08:00:00Z\","
            + "\"updatedAt\":\"2026-10-03T08:00:00Z\"}]";

    List<ExpenseDto> list = gson.fromJson(json, new TypeToken<List<ExpenseDto>>() {}.getType());

    assertEquals(2, list.size());
    assertEquals(42L, list.get(0).id);
    assertEquals("12.50", list.get(0).amount.toString());
    assertEquals(LocalDate.parse("2026-10-21"), list.get(0).date);
    assertEquals(Instant.parse("2026-10-21T12:34:56Z"), list.get(0).createdAt);
    assertEquals("8.00", list.get(1).amount.toString());
    assertNull(list.get(1).note);
  }

  @Test
  public void monthlySummaryExample() {
    String json =
        "{\"month\":\"2026-10\",\"currency\":\"USD\",\"total\":245.50,\"count\":7,"
            + "\"categories\":[{\"category\":\"HOUSING\",\"total\":150.00,\"count\":1},"
            + "{\"category\":\"FOOD\",\"total\":60.50,\"count\":4}]}";

    MonthlySummaryDto summary = gson.fromJson(json, MonthlySummaryDto.class);

    assertEquals(YearMonth.of(2026, 10), summary.month);
    assertEquals("245.50", summary.total.toString());
    assertEquals(7L, summary.count);
    assertEquals("HOUSING", summary.categories.get(0).category);
    assertEquals("150.00", summary.categories.get(0).total.toString());
    assertEquals(4L, summary.categories.get(1).count);
  }

  @Test
  public void emptyMonthSummary() {
    String json =
        "{\"month\":\"2026-09\",\"currency\":\"USD\",\"total\":0.00,\"count\":0,\"categories\":[]}";

    MonthlySummaryDto summary = gson.fromJson(json, MonthlySummaryDto.class);

    assertEquals("0.00", summary.total.toString());
    assertEquals(0, summary.categories.size());
  }

  @Test
  public void metadataExample() {
    String json =
        "{\"currency\":\"USD\",\"categories\":[{\"code\":\"FOOD\",\"displayName\":\"Food\"},"
            + "{\"code\":\"OTHER\",\"displayName\":\"Other\"}]}";

    MetadataDto metadata = gson.fromJson(json, MetadataDto.class);

    assertEquals("USD", metadata.currency);
    assertEquals("Food", metadata.displayName("FOOD"));
    assertEquals("UNKNOWN", metadata.displayName("UNKNOWN"));
  }

  @Test
  public void problemExample() {
    String json =
        "{\"type\":\"/problems/validation-error\",\"title\":\"Validation failed\",\"status\":400,"
            + "\"detail\":\"Request has 2 invalid fields\",\"instance\":\"/api/v1/expenses\","
            + "\"errors\":[{\"field\":\"amount\",\"message\":\"must be greater than 0\"}]}";

    ProblemDto problem = gson.fromJson(json, ProblemDto.class);

    assertEquals(400, problem.status);
    assertEquals("amount", problem.errors.get(0).field);
  }

  @Test
  public void requestSerializesAmountAsNumberWithScaleTwoAndIsoDate() {
    ExpenseRequestDto request =
        new ExpenseRequestDto(
            new BigDecimal("45").setScale(2), "TRANSPORT", LocalDate.parse("2026-10-20"), null);

    assertEquals(
        "{\"amount\":45.00,\"category\":\"TRANSPORT\",\"date\":\"2026-10-20\"}",
        gson.toJson(request));
  }
}
