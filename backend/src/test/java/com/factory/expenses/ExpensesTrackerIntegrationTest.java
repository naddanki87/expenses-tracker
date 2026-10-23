package com.factory.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:itest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "app.currency=EUR"
    })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ExpensesTrackerIntegrationTest {

  @Autowired private MockMvc mvc;

  private String body(String amount, String category, String date, String note) {
    return """
        {"amount": %s, "category": "%s", "date": "%s", "note": %s}
        """
        .formatted(amount, category, date, note);
  }

  private void create(String amount, String category, String date) throws Exception {
    mvc.perform(
            post("/api/v1/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(amount, category, date, "null")))
        .andExpect(status().isCreated());
  }

  @Test
  void healthIsUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void fullLifecycleCreateReadUpdateDelete(CapturedOutput output) throws Exception {
    String today = LocalDate.now().toString();
    String response =
        mvc.perform(
                post("/api/v1/expenses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("45", "TRANSPORT", today, "\"  Super secret note  \"")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(45.00))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"amount\":45.00")))
            .andExpect(jsonPath("$.note").value("Super secret note"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = response.replaceAll(".*\"id\":(\\d+).*", "$1");

    mvc.perform(get("/api/v1/expenses/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").value("TRANSPORT"));

    mvc.perform(
            put("/api/v1/expenses/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("14.5", "FOOD", today, "null")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(14.50))
        .andExpect(jsonPath("$.category").value("FOOD"))
        .andExpect(jsonPath("$.note").doesNotExist());

    mvc.perform(delete("/api/v1/expenses/" + id)).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/expenses/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("/problems/not-found"));

    assertThat(output.getOut())
        .contains("POST /api/v1/expenses -> 201")
        .contains("GET /api/v1/expenses/" + id + " -> 404")
        .doesNotContain("Super secret note");
  }

  @Test
  void filtersAndSummarisesByMonth() throws Exception {
    create("12.50", "FOOD", "2020-03-03");
    create("48.00", "FOOD", "2020-03-31");
    create("150.00", "HOUSING", "2020-03-01");
    create("35.00", "TRANSPORT", "2020-03-15");
    create("99.00", "FOOD", "2020-02-29");
    create("99.00", "FOOD", "2020-04-01");

    mvc.perform(get("/api/v1/expenses").param("month", "2020-03"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].date").value("2020-03-31"));
    mvc.perform(get("/api/v1/expenses").param("month", "2020-03").param("category", "FOOD"))
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/v1/expenses").param("month", "1999-01"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    mvc.perform(get("/api/v1/summaries/monthly").param("month", "2020-03"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.total").value(245.50))
        .andExpect(jsonPath("$.count").value(4))
        .andExpect(jsonPath("$.categories[0].category").value("HOUSING"))
        .andExpect(jsonPath("$.categories[1].category").value("FOOD"))
        .andExpect(jsonPath("$.categories[1].total").value(60.50))
        .andExpect(jsonPath("$.categories[1].count").value(2))
        .andExpect(jsonPath("$.categories[2].category").value("TRANSPORT"));

    mvc.perform(get("/api/v1/summaries/monthly").param("month", "1999-01"))
        .andExpect(jsonPath("$.total").value(0.00))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"total\":0.00")))
        .andExpect(jsonPath("$.count").value(0))
        .andExpect(jsonPath("$.categories.length()").value(0));
  }

  @Test
  void futureDateIsRejected() throws Exception {
    mvc.perform(
            post("/api/v1/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("5", "FOOD", LocalDate.now().plusDays(1).toString(), "null")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("date"));
  }

  @Test
  void metadataExposesConfiguredCurrency() throws Exception {
    mvc.perform(get("/api/v1/metadata"))
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.categories.length()").value(11));
  }
}
