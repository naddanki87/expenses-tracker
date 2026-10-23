package com.factory.expenses.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.ExpenseRequestDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import com.factory.expenses.testing.FakeBaseUrlStore;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Repository against MockWebServer using the examples of docs/openapi.yaml. */
public class ExpenseRepositoryTest {

  private static final String EXPENSE_JSON =
      "{\"id\":42,\"amount\":12.50,\"category\":\"FOOD\",\"date\":\"2026-10-21\","
          + "\"note\":\"Lunch with team\",\"createdAt\":\"2026-10-21T12:34:56Z\","
          + "\"updatedAt\":\"2026-10-21T12:34:56Z\"}";
  private static final String METADATA_JSON =
      "{\"currency\":\"USD\",\"categories\":[{\"code\":\"FOOD\",\"displayName\":\"Food\"}]}";

  private MockWebServer server;
  private FakeBaseUrlStore store;
  private ExpenseRepository repository;

  @Before
  public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    store = new FakeBaseUrlStore(server.url("/").toString());
    repository = new ExpenseRepository(store, new OkHttpClient());
  }

  @After
  public void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  public void listExpensesSendsMonthAndCategory() throws Exception {
    server.enqueue(json(200, "[" + EXPENSE_JSON + "]"));
    Await<List<ExpenseDto>> result = new Await<>();

    repository.listExpenses(YearMonth.of(2026, 10), "FOOD", result);
    result.await();

    RecordedRequest request = server.takeRequest();
    assertEquals("GET", request.getMethod());
    assertEquals("/api/v1/expenses?month=2026-10&category=FOOD", request.getPath());
    assertEquals(1, result.value.size());
    assertEquals("12.50", result.value.get(0).amount.toString());
  }

  @Test
  public void listExpensesOmitsAbsentFilters() throws Exception {
    server.enqueue(json(200, "[]"));
    Await<List<ExpenseDto>> result = new Await<>();

    repository.listExpenses(null, null, result);
    result.await();

    assertEquals("/api/v1/expenses", server.takeRequest().getPath());
    assertTrue(result.value.isEmpty());
  }

  @Test
  public void createExpensePostsJsonBody() throws Exception {
    server.enqueue(json(201, EXPENSE_JSON));
    Await<ExpenseDto> result = new Await<>();
    ExpenseRequestDto body =
        new ExpenseRequestDto(
            new BigDecimal("12.50"), "FOOD", LocalDate.parse("2026-10-21"), "Lunch with team");

    repository.createExpense(body, result);
    result.await();

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("/api/v1/expenses", request.getPath());
    assertEquals(
        "{\"amount\":12.50,\"category\":\"FOOD\",\"date\":\"2026-10-21\","
            + "\"note\":\"Lunch with team\"}",
        request.getBody().readUtf8());
    assertEquals(42L, result.value.id);
  }

  @Test
  public void getAndUpdateAndDeleteUseExpensePath() throws Exception {
    server.enqueue(json(200, EXPENSE_JSON));
    server.enqueue(json(200, EXPENSE_JSON));
    server.enqueue(new MockResponse().setResponseCode(204));
    Await<ExpenseDto> get = new Await<>();
    Await<ExpenseDto> update = new Await<>();
    Await<Void> delete = new Await<>();

    repository.getExpense(42, get);
    get.await();
    repository.updateExpense(
        42,
        new ExpenseRequestDto(new BigDecimal("14.00"), "FOOD", LocalDate.parse("2026-10-21"), null),
        update);
    update.await();
    repository.deleteExpense(42, delete);
    delete.await();

    RecordedRequest first = server.takeRequest();
    RecordedRequest second = server.takeRequest();
    RecordedRequest third = server.takeRequest();
    assertEquals("GET /api/v1/expenses/42", first.getMethod() + " " + first.getPath());
    assertEquals("PUT /api/v1/expenses/42", second.getMethod() + " " + second.getPath());
    assertEquals("DELETE /api/v1/expenses/42", third.getMethod() + " " + third.getPath());
    assertNull(delete.error);
  }

  @Test
  public void monthlySummaryRequestsRequiredMonth() throws Exception {
    server.enqueue(
        json(
            200,
            "{\"month\":\"2026-10\",\"currency\":\"USD\",\"total\":245.50,\"count\":7,"
                + "\"categories\":[{\"category\":\"FOOD\",\"total\":60.50,\"count\":4}]}"));
    Await<MonthlySummaryDto> result = new Await<>();

    repository.getMonthlySummary(YearMonth.of(2026, 10), result);
    result.await();

    assertEquals("/api/v1/summaries/monthly?month=2026-10", server.takeRequest().getPath());
    assertEquals("245.50", result.value.total.toString());
  }

  @Test
  public void metadataIsCachedUntilBaseUrlChanges() throws Exception {
    server.enqueue(json(200, METADATA_JSON));
    Await<MetadataDto> first = new Await<>();
    repository.getMetadata(first);
    first.await();
    Await<MetadataDto> second = new Await<>();
    repository.getMetadata(second);
    second.await();

    assertEquals(1, server.getRequestCount());
    assertEquals("USD", second.value.currency);

    MockWebServer other = new MockWebServer();
    other.start();
    try {
      other.enqueue(json(200, METADATA_JSON));
      store.setBaseUrl(other.url("/").toString());
      Await<MetadataDto> third = new Await<>();
      repository.getMetadata(third);
      third.await();
      assertEquals(1, other.getRequestCount());
    } finally {
      other.shutdown();
    }
  }

  @Test
  public void validationProblemMapsToFieldErrors() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/problem+json")
            .setBody(
                "{\"type\":\"/problems/validation-error\",\"title\":\"Validation"
                    + " failed\",\"status\":400,\"detail\":\"Request has 1 invalid"
                    + " field\",\"errors\":[{\"field\":\"amount\",\"message\":\"must be greater"
                    + " than 0\"}]}"));
    Await<ExpenseDto> result = new Await<>();

    repository.createExpense(
        new ExpenseRequestDto(BigDecimal.ONE, "FOOD", LocalDate.parse("2026-10-21"), null), result);
    result.await();

    assertEquals(ApiError.Kind.VALIDATION, result.error.getKind());
    assertEquals(400, result.error.getStatus());
    assertEquals("Validation failed", result.error.getTitle());
    assertEquals("Request has 1 invalid field", result.error.getDetail());
    assertEquals("amount", result.error.getFieldErrors().get(0).field);
    assertEquals("must be greater than 0", result.error.getFieldErrors().get(0).message);
  }

  @Test
  public void notFoundMapsToNotFoundKind() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setBody(
                "{\"type\":\"/problems/not-found\",\"title\":\"Not found\",\"status\":404,"
                    + "\"detail\":\"Expense 999 not found\"}"));
    Await<ExpenseDto> result = new Await<>();

    repository.getExpense(999, result);
    result.await();

    assertEquals(ApiError.Kind.NOT_FOUND, result.error.getKind());
    assertTrue(result.error.getFieldErrors().isEmpty());
  }

  @Test
  public void serverErrorWithUnparsableBodyStillMapsToServerKind() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("<html>boom</html>"));
    Await<ExpenseDto> result = new Await<>();

    repository.getExpense(1, result);
    result.await();

    assertEquals(ApiError.Kind.SERVER, result.error.getKind());
    assertNull(result.error.getTitle());
  }

  @Test
  public void unexpectedStatusMapsToUnknownKind() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(415));
    Await<ExpenseDto> result = new Await<>();

    repository.getExpense(1, result);
    result.await();

    assertEquals(ApiError.Kind.UNKNOWN, result.error.getKind());
  }

  @Test
  public void connectionFailureMapsToNetworkKind() throws Exception {
    server.shutdown();
    Await<ExpenseDto> result = new Await<>();

    repository.getExpense(1, result);
    result.await();

    assertEquals(ApiError.Kind.NETWORK, result.error.getKind());
  }

  @Test
  public void malformedBaseUrlMapsToNetworkKindWithoutThrowing() throws Exception {
    store.setBaseUrl("not a url");
    Await<ExpenseDto> result = new Await<>();

    repository.getExpense(1, result);

    assertNotNull(result.error);
    assertEquals(ApiError.Kind.NETWORK, result.error.getKind());
  }

  private static MockResponse json(int status, String body) {
    return new MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body);
  }

  /** Callback that lets the test thread wait for the OkHttp thread. */
  private static final class Await<T> implements ExpenseRepository.Callback<T> {
    private final CountDownLatch latch = new CountDownLatch(1);
    T value;
    ApiError error;

    @Override
    public void onSuccess(T value) {
      this.value = value;
      latch.countDown();
    }

    @Override
    public void onError(ApiError error) {
      this.error = error;
      latch.countDown();
    }

    void await() throws InterruptedException {
      assertTrue("no callback within 5 s", latch.await(5, TimeUnit.SECONDS));
    }
  }
}
