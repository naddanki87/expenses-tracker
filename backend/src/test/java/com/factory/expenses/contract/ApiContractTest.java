package com.factory.expenses.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Calls every operation and documented status code and validates the exchange against the contract.
 */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ApiContractTest {

  private static final String EXPENSES = "/api/v1/expenses";
  private static final String SUMMARY = "/api/v1/summaries/monthly";
  private static final String VALID =
      "{\"amount\": 12.5, \"category\": \"FOOD\", \"date\": \"2026-10-01\", \"note\": \"Lunch\"}";
  private static final String UNKNOWN_ID = "/987654";

  private static OpenApiInteractionValidator validator;

  @Autowired private MockMvc mvc;

  @BeforeAll
  static void loadContract() {
    validator = OpenApiInteractionValidator.createFor("../docs/openapi.yaml").build();
  }

  private ValidationReport exchange(
      MockHttpServletRequestBuilder builder,
      String template,
      Request.Method method,
      String body,
      String contentType,
      Map<String, String> query)
      throws Exception {
    MockHttpServletRequestBuilder request = builder;
    if (body != null) {
      request = request.contentType(contentType).content(body);
    }
    for (Map.Entry<String, String> param : query.entrySet()) {
      request = request.param(param.getKey(), param.getValue());
    }
    MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
    return validator.validateResponse(template, method, toContractResponse(response));
  }

  private static SimpleResponse toContractResponse(MockHttpServletResponse response)
      throws Exception {
    SimpleResponse.Builder builder = new SimpleResponse.Builder(response.getStatus());
    if (response.getContentType() != null) {
      builder.withContentType(response.getContentType());
    }
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    if (!body.isEmpty()) {
      builder.withBody(body);
    }
    return builder.build();
  }

  private void assertValid(ValidationReport report) {
    assertThat(report.hasErrors()).as(report.getMessages().toString()).isFalse();
  }

  private static String template(String path) {
    return path.replaceAll("^(/api/v1/expenses)/[^/]+$", "$1/{id}");
  }

  private ValidationReport getOf(String path, Map<String, String> query) throws Exception {
    return exchange(get(path), template(path), Request.Method.GET, null, null, query);
  }

  private ValidationReport deleteOf(String path) throws Exception {
    return exchange(delete(path), template(path), Request.Method.DELETE, null, null, Map.of());
  }

  private ValidationReport postOf(String body, String contentType) throws Exception {
    return exchange(post(EXPENSES), EXPENSES, Request.Method.POST, body, contentType, Map.of());
  }

  private ValidationReport putOf(String path, String body, String contentType) throws Exception {
    return exchange(put(path), template(path), Request.Method.PUT, body, contentType, Map.of());
  }

  private String createExpense() throws Exception {
    String json =
        mvc.perform(post(EXPENSES).contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return EXPENSES + "/" + json.replaceAll(".*\"id\":(\\d+).*", "$1");
  }

  @Test
  void listExpenses200() throws Exception {
    createExpense();
    assertValid(getOf(EXPENSES, Map.of()));
  }

  @Test
  void listExpensesWithFilters200() throws Exception {
    createExpense();
    assertValid(getOf(EXPENSES, Map.of("month", "2026-10", "category", "FOOD")));
  }

  @Test
  void listExpensesInvalidMonth400() throws Exception {
    assertValid(getOf(EXPENSES, Map.of("month", "2026-13")));
  }

  @Test
  void listExpensesInvalidCategory400() throws Exception {
    assertValid(getOf(EXPENSES, Map.of("category", "PETS")));
  }

  @Test
  void createExpense201() throws Exception {
    assertValid(postOf(VALID, MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void createExpenseInvalid400() throws Exception {
    assertValid(postOf("{\"amount\": -1}", MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void createExpenseMalformed400() throws Exception {
    assertValid(postOf("{not json", MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void createExpenseWrongMediaType415() throws Exception {
    assertValid(postOf("hello", MediaType.TEXT_PLAIN_VALUE));
  }

  @Test
  void getExpense200() throws Exception {
    assertValid(getOf(createExpense(), Map.of()));
  }

  @Test
  void getExpenseBadId400() throws Exception {
    assertValid(getOf(EXPENSES + "/abc", Map.of()));
  }

  @Test
  void getExpenseUnknown404() throws Exception {
    assertValid(getOf(EXPENSES + UNKNOWN_ID, Map.of()));
  }

  @Test
  void updateExpense200() throws Exception {
    assertValid(putOf(createExpense(), VALID, MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void updateExpenseInvalid400() throws Exception {
    assertValid(putOf(createExpense(), "{\"amount\": 0}", MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void updateExpenseUnknown404() throws Exception {
    assertValid(putOf(EXPENSES + UNKNOWN_ID, VALID, MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void updateExpenseWrongMediaType415() throws Exception {
    assertValid(putOf(createExpense(), "hello", MediaType.TEXT_PLAIN_VALUE));
  }

  @Test
  void deleteExpense204() throws Exception {
    assertValid(deleteOf(createExpense()));
  }

  @Test
  void deleteExpenseBadId400() throws Exception {
    assertValid(deleteOf(EXPENSES + "/abc"));
  }

  @Test
  void deleteExpenseUnknown404() throws Exception {
    assertValid(deleteOf(EXPENSES + UNKNOWN_ID));
  }

  @Test
  void monthlySummary200() throws Exception {
    createExpense();
    assertValid(getOf(SUMMARY, Map.of("month", "2026-10")));
  }

  @Test
  void monthlySummaryMissingMonth400() throws Exception {
    assertValid(getOf(SUMMARY, Map.of()));
  }

  @Test
  void metadata200() throws Exception {
    assertValid(getOf("/api/v1/metadata", Map.of()));
  }

  @Test
  void validatorRejectsResponseMissingRequiredId() {
    SimpleResponse broken =
        SimpleResponse.Builder.ok()
            .withContentType(MediaType.APPLICATION_JSON_VALUE)
            .withBody(
                "{\"amount\":12.50,\"category\":\"FOOD\",\"date\":\"2026-10-01\","
                    + "\"createdAt\":\"2026-10-01T10:00:00Z\",\"updatedAt\":\"2026-10-01T10:00:00Z\"}")
            .build();
    ValidationReport report =
        validator.validateResponse(EXPENSES + "/1", Request.Method.GET, broken);
    assertThat(report.hasErrors()).isTrue();
  }
}
