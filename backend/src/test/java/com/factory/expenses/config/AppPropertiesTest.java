package com.factory.expenses.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppPropertiesTest {

  @Configuration
  @EnableConfigurationProperties(AppProperties.class)
  static class Config {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Config.class);

  @Test
  void defaultsToUsd() {
    runner.run(ctx -> assertThat(ctx.getBean(AppProperties.class).currency()).isEqualTo("USD"));
  }

  @Test
  void acceptsThreeUppercaseLetters() {
    runner
        .withPropertyValues("app.currency=EUR")
        .run(ctx -> assertThat(ctx.getBean(AppProperties.class).currency()).isEqualTo("EUR"));
  }

  @Test
  void rejectsInvalidCurrency() {
    for (String bad : new String[] {"usd1", "usd", "US", "EURO"}) {
      runner.withPropertyValues("app.currency=" + bad).run(ctx -> assertThat(ctx).hasFailed());
    }
  }
}
