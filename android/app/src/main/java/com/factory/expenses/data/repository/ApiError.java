package com.factory.expenses.data.repository;

import com.factory.expenses.data.api.dto.FieldErrorDto;
import com.factory.expenses.data.api.dto.ProblemDto;
import java.util.Collections;
import java.util.List;

/** Failure of an API call, already classified so ViewModels never see HTTP details. */
public final class ApiError {

  /** Coarse classification used to choose the message and the recovery action. */
  public enum Kind {
    NETWORK,
    VALIDATION,
    NOT_FOUND,
    SERVER,
    UNKNOWN
  }

  private final Kind kind;
  private final int status;
  private final String title;
  private final String detail;
  private final List<FieldErrorDto> fieldErrors;

  public ApiError(
      Kind kind, int status, String title, String detail, List<FieldErrorDto> fieldErrors) {
    this.kind = kind;
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.fieldErrors = fieldErrors == null ? Collections.emptyList() : fieldErrors;
  }

  public static ApiError network(String message) {
    return new ApiError(Kind.NETWORK, 0, "Network error", message, null);
  }

  /** Builds an error from an HTTP status and an optional parsed problem body. */
  public static ApiError fromHttp(int status, ProblemDto problem) {
    Kind kind;
    if (status == 404) {
      kind = Kind.NOT_FOUND;
    } else if (status == 400) {
      kind = Kind.VALIDATION;
    } else if (status >= 500) {
      kind = Kind.SERVER;
    } else {
      kind = Kind.UNKNOWN;
    }
    if (problem == null) {
      return new ApiError(kind, status, null, null, null);
    }
    return new ApiError(kind, status, problem.title, problem.detail, problem.errors);
  }

  public Kind getKind() {
    return kind;
  }

  public int getStatus() {
    return status;
  }

  public String getTitle() {
    return title;
  }

  public String getDetail() {
    return detail;
  }

  public List<FieldErrorDto> getFieldErrors() {
    return fieldErrors;
  }
}
