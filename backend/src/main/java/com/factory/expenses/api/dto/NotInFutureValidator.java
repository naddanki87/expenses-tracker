package com.factory.expenses.api.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.LocalDate;

/** Compares against the injected Clock so tests can fix today's date. */
public class NotInFutureValidator implements ConstraintValidator<NotInFuture, LocalDate> {

  private final Clock clock;

  public NotInFutureValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
    return value == null || !value.isAfter(LocalDate.now(clock));
  }
}
