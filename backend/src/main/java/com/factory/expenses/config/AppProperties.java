package com.factory.expenses.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app")
public record AppProperties(
    @DefaultValue("USD")
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO 4217 code")
        String currency) {}
