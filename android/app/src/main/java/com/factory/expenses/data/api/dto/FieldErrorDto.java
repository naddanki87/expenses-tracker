package com.factory.expenses.data.api.dto;

/** One entry of the {@code errors} array of a problem response. */
public final class FieldErrorDto {
  public final String field;
  public final String message;

  public FieldErrorDto(String field, String message) {
    this.field = field;
    this.message = message;
  }
}
