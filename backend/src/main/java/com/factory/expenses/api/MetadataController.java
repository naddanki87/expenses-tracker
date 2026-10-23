package com.factory.expenses.api;

import com.factory.expenses.api.dto.CategoryInfo;
import com.factory.expenses.api.dto.MetadataResponse;
import com.factory.expenses.config.AppProperties;
import com.factory.expenses.domain.Category;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataController {

  private final AppProperties properties;

  public MetadataController(AppProperties properties) {
    this.properties = properties;
  }

  @GetMapping
  public MetadataResponse metadata() {
    return new MetadataResponse(
        properties.currency(),
        Arrays.stream(Category.values())
            .map(c -> new CategoryInfo(c.name(), c.displayName()))
            .toList());
  }
}
