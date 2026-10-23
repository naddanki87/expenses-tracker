package com.factory.expenses.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.factory.expenses.api.dto.CategoryTotal;
import com.factory.expenses.api.dto.MonthlySummaryResponse;
import com.factory.expenses.config.AppProperties;
import com.factory.expenses.domain.Category;
import com.factory.expenses.service.SummaryService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({SummaryController.class, MetadataController.class})
@Import(SummaryAndMetadataControllerTest.Props.class)
class SummaryAndMetadataControllerTest {

  @TestConfiguration
  static class Props {
    @Bean
    AppProperties appProperties() {
      return new AppProperties("EUR");
    }
  }

  @Autowired private MockMvc mvc;
  @MockitoBean private SummaryService summaryService;

  @Test
  void monthlyReturnsSummary() throws Exception {
    when(summaryService.monthly(YearMonth.of(2026, 10)))
        .thenReturn(
            new MonthlySummaryResponse(
                "2026-10",
                "EUR",
                new BigDecimal("150.00"),
                1,
                List.of(new CategoryTotal(Category.HOUSING, new BigDecimal("150.00"), 1))));

    mvc.perform(get("/api/v1/summaries/monthly").param("month", "2026-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.month").value("2026-10"))
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.total").value(150.00))
        .andExpect(jsonPath("$.categories[0].category").value("HOUSING"));
  }

  @Test
  void monthlyWithoutMonthIs400() throws Exception {
    mvc.perform(get("/api/v1/summaries/monthly"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("/problems/validation-error"))
        .andExpect(jsonPath("$.detail").value("Request has 1 invalid parameter"))
        .andExpect(jsonPath("$.errors[0].field").value("month"));
  }

  @Test
  void monthlyWithInvalidMonthIs400() throws Exception {
    mvc.perform(get("/api/v1/summaries/monthly").param("month", "2026-13"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("month"))
        .andExpect(jsonPath("$.errors[0].message").value("must be a month in the form YYYY-MM"));
  }

  @Test
  void metadataListsCurrencyAndAllCategoriesInOrder() throws Exception {
    mvc.perform(get("/api/v1/metadata"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.categories.length()").value(11))
        .andExpect(jsonPath("$.categories[0].code").value("FOOD"))
        .andExpect(jsonPath("$.categories[0].displayName").value("Food"))
        .andExpect(jsonPath("$.categories[10].code").value("OTHER"))
        .andExpect(jsonPath("$.categories[6].displayName").value("Entertainment"));
  }
}
