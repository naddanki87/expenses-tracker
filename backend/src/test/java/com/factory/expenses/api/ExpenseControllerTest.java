package com.factory.expenses.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import com.factory.expenses.service.ExpenseNotFoundException;
import com.factory.expenses.service.ExpenseService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpenseController.class)
@Import(ExpenseControllerTest.FixedClock.class)
class ExpenseControllerTest {

  @TestConfiguration
  static class FixedClock {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    }
  }

  private static final String VALID =
      """
      {"amount": 12.5, "category": "FOOD", "date": "2026-10-21", "note": "  Lunch  "}
      """;

  @Autowired private MockMvc mvc;
  @MockitoBean private ExpenseService service;

  private static Expense expense(long id) {
    Expense e =
        new Expense(
            new BigDecimal("12.50"),
            Category.FOOD,
            LocalDate.of(2026, 10, 21),
            "Lunch",
            Instant.parse("2026-10-21T12:34:56Z"));
    ReflectionTestUtils.setField(e, "id", id);
    return e;
  }

  private org.springframework.test.web.servlet.ResultActions send(String body) throws Exception {
    return mvc.perform(
        post("/api/v1/expenses").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  void createReturns201WithLocationAndBody() throws Exception {
    when(service.create(any())).thenReturn(expense(42));

    send(VALID)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/expenses/42"))
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.amount").value(12.50))
        .andExpect(jsonPath("$.category").value("FOOD"))
        .andExpect(jsonPath("$.date").value("2026-10-21"))
        .andExpect(jsonPath("$.createdAt").value("2026-10-21T12:34:56Z"));
  }

  @Test
  void createSerializesAmountWithScaleTwo() throws Exception {
    when(service.create(any())).thenReturn(expense(1));

    send(VALID)
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"amount\":12.50")));
  }

  @Test
  void createAcceptsTodayAndRejectsTomorrow() throws Exception {
    when(service.create(any())).thenReturn(expense(1));

    send(VALID.replace("2026-10-21", "2026-10-21")).andExpect(status().isCreated());
    send(VALID.replace("2026-10-21", "2026-10-22"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("date"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be in the future"));
  }

  @Test
  void createRejectsInvalidAmounts() throws Exception {
    for (String amount : List.of("0", "-1", "10000000000", "1.234", "9999999999.991")) {
      send(VALID.replace("12.5", amount))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
          .andExpect(jsonPath("$.type").value("/problems/validation-error"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }
  }

  @Test
  void createAcceptsBoundaryAmounts() throws Exception {
    when(service.create(any())).thenReturn(expense(1));

    send(VALID.replace("12.5", "0.01")).andExpect(status().isCreated());
    send(VALID.replace("12.5", "9999999999.99")).andExpect(status().isCreated());
  }

  @Test
  void createListsEveryMissingField() throws Exception {
    send("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("/problems/validation-error"))
        .andExpect(jsonPath("$.detail").value("Request has 3 invalid fields"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(org.hamcrest.Matchers.containsInAnyOrder("amount", "category", "date")))
        .andExpect(jsonPath("$.instance").value("/api/v1/expenses"));
  }

  @Test
  void createRejectsNoteOver500Characters() throws Exception {
    send(VALID.replace("  Lunch  ", "x".repeat(501)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("note"));
    when(service.create(any())).thenReturn(expense(1));
    send(VALID.replace("  Lunch  ", "x".repeat(500))).andExpect(status().isCreated());
  }

  @Test
  void createTrimsNoteAndTurnsBlankIntoNull() throws Exception {
    when(service.create(any())).thenReturn(expense(1));

    send(VALID).andExpect(status().isCreated());
    send(VALID.replace("  Lunch  ", "   ")).andExpect(status().isCreated());

    verify(service)
        .create(
            eq(
                new com.factory.expenses.api.dto.ExpenseRequest(
                    new BigDecimal("12.5"), Category.FOOD, LocalDate.of(2026, 10, 21), "Lunch")));
    verify(service)
        .create(
            eq(
                new com.factory.expenses.api.dto.ExpenseRequest(
                    new BigDecimal("12.5"), Category.FOOD, LocalDate.of(2026, 10, 21), null)));
  }

  @Test
  void createRejectsUnknownCategoryAsMalformed() throws Exception {
    send(VALID.replace("FOOD", "PETS"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value("/problems/malformed-request"))
        .andExpect(jsonPath("$.errors[0].field").value("category"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value(org.hamcrest.Matchers.startsWith("must be one of FOOD, GROCERIES")));
  }

  @Test
  void createRejectsMalformedJson() throws Exception {
    send("{not json")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("/problems/malformed-request"))
        .andExpect(jsonPath("$.title").value("Malformed request"));
  }

  @Test
  void createRejectsBadDateAsMalformed() throws Exception {
    send(VALID.replace("2026-10-21", "21/10/2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("/problems/malformed-request"))
        .andExpect(jsonPath("$.errors[0].field").value("date"));
  }

  @Test
  void createRejectsNonJsonContentType() throws Exception {
    mvc.perform(post("/api/v1/expenses").contentType(MediaType.TEXT_PLAIN).content("hello"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(415));
  }

  @Test
  void getReturnsExpense() throws Exception {
    when(service.get(42)).thenReturn(expense(42));

    mvc.perform(get("/api/v1/expenses/42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.note").value("Lunch"));
  }

  @Test
  void getUnknownReturns404ProblemDetails() throws Exception {
    when(service.get(999)).thenThrow(new ExpenseNotFoundException(999));

    mvc.perform(get("/api/v1/expenses/999"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value("/problems/not-found"))
        .andExpect(jsonPath("$.title").value("Not found"))
        .andExpect(jsonPath("$.detail").value("Expense 999 not found"))
        .andExpect(jsonPath("$.instance").value("/api/v1/expenses/999"));
  }

  @Test
  void getWithNonNumericIdReturns400() throws Exception {
    mvc.perform(get("/api/v1/expenses/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("/problems/validation-error"))
        .andExpect(jsonPath("$.errors[0].field").value("id"))
        .andExpect(jsonPath("$.errors[0].message").value("must be a number"));
  }

  @Test
  void updateReturnsReplacedExpense() throws Exception {
    when(service.update(eq(42L), any())).thenReturn(expense(42));

    mvc.perform(put("/api/v1/expenses/42").contentType(MediaType.APPLICATION_JSON).content(VALID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42));
  }

  @Test
  void updateAppliesSameValidation() throws Exception {
    mvc.perform(
            put("/api/v1/expenses/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("12.5", "0")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("amount"));
  }

  @Test
  void updateUnknownReturns404() throws Exception {
    when(service.update(eq(5L), any())).thenThrow(new ExpenseNotFoundException(5));

    mvc.perform(put("/api/v1/expenses/5").contentType(MediaType.APPLICATION_JSON).content(VALID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("/problems/not-found"));
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/api/v1/expenses/42")).andExpect(status().isNoContent());

    verify(service).delete(42L);
  }

  @Test
  void deleteUnknownReturns404() throws Exception {
    doThrow(new ExpenseNotFoundException(5)).when(service).delete(5L);

    mvc.perform(delete("/api/v1/expenses/5"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("/problems/not-found"));
  }

  @Test
  void listPassesFiltersToService() throws Exception {
    when(service.list(Optional.of(YearMonth.of(2026, 10)), Optional.of(Category.FOOD)))
        .thenReturn(List.of(expense(2), expense(1)));

    mvc.perform(get("/api/v1/expenses").param("month", "2026-10").param("category", "FOOD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(2));
  }

  @Test
  void listWithoutParamsReturnsEmptyArray() throws Exception {
    when(service.list(Optional.empty(), Optional.empty())).thenReturn(List.of());

    mvc.perform(get("/api/v1/expenses")).andExpect(status().isOk()).andExpect(content().json("[]"));
  }

  @Test
  void listRejectsBadMonthAndCategory() throws Exception {
    for (String month : List.of("2026-13", "2026-1", "abc", "2026-10-01")) {
      mvc.perform(get("/api/v1/expenses").param("month", month))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value("/problems/validation-error"))
          .andExpect(jsonPath("$.errors[0].field").value("month"))
          .andExpect(jsonPath("$.errors[0].message").value("must be a month in the form YYYY-MM"));
    }
    mvc.perform(get("/api/v1/expenses").param("category", "PETS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("/problems/validation-error"))
        .andExpect(jsonPath("$.errors[0].field").value("category"));
  }

  @Test
  void unsupportedMethodReturns405ProblemDetails() throws Exception {
    mvc.perform(delete("/api/v1/expenses"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void unexpectedErrorReturns500WithGenericDetail() throws Exception {
    when(service.get(1)).thenThrow(new IllegalStateException("secret internals"));

    mvc.perform(get("/api/v1/expenses/1"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
  }
}
