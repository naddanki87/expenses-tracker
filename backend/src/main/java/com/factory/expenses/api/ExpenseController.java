package com.factory.expenses.api;

import com.factory.expenses.api.dto.ExpenseRequest;
import com.factory.expenses.api.dto.ExpenseResponse;
import com.factory.expenses.domain.Category;
import com.factory.expenses.domain.Expense;
import com.factory.expenses.service.ExpenseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

  private final ExpenseService service;

  public ExpenseController(ExpenseService service) {
    this.service = service;
  }

  @GetMapping
  public List<ExpenseResponse> list(
      @RequestParam(name = "month", required = false) String month,
      @RequestParam(name = "category", required = false) Category category) {
    return service
        .list(Optional.ofNullable(month).map(MonthParser::parse), Optional.ofNullable(category))
        .stream()
        .map(ExpenseResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
    Expense created = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/expenses/" + created.getId()))
        .body(ExpenseResponse.from(created));
  }

  @GetMapping("/{id}")
  public ExpenseResponse get(@PathVariable long id) {
    return ExpenseResponse.from(service.get(id));
  }

  @PutMapping("/{id}")
  public ExpenseResponse update(@PathVariable long id, @Valid @RequestBody ExpenseRequest request) {
    return ExpenseResponse.from(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
