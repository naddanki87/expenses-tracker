package com.factory.expenses.api;

public class InvalidParameterException extends RuntimeException {

  private final String field;

  public InvalidParameterException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
