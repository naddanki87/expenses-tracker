package com.factory.expenses.api;

import com.factory.expenses.api.dto.MonthlySummaryResponse;
import com.factory.expenses.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/summaries")
public class SummaryController {

  private final SummaryService service;

  public SummaryController(SummaryService service) {
    this.service = service;
  }

  @GetMapping("/monthly")
  public MonthlySummaryResponse monthly(@RequestParam("month") String month) {
    return service.monthly(MonthParser.parse(month));
  }
}
