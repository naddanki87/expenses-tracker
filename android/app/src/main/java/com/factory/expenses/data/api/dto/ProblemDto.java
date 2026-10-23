package com.factory.expenses.data.api.dto;

import java.util.List;

/** RFC 9457 problem details as returned by the API. */
public final class ProblemDto {
  public final String type;
  public final String title;
  public final int status;
  public final String detail;
  public final String instance;
  public final List<FieldErrorDto> errors;

  public ProblemDto(
      String type,
      String title,
      int status,
      String detail,
      String instance,
      List<FieldErrorDto> errors) {
    this.type = type;
    this.title = title;
    this.status = status;
    this.detail = detail;
    this.instance = instance;
    this.errors = errors;
  }
}
