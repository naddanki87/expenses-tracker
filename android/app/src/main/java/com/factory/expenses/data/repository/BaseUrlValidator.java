package com.factory.expenses.data.repository;

import java.util.Optional;
import okhttp3.HttpUrl;

/** Validates and normalises the base URL typed in Settings. */
public final class BaseUrlValidator {

  private BaseUrlValidator() {}

  /** Returns the URL with a trailing slash, or empty when it is not an http(s) URL. */
  public static Optional<String> normalize(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String trimmed = input.trim();
    if (trimmed.isEmpty() || HttpUrl.parse(trimmed) == null) {
      return Optional.empty();
    }
    return Optional.of(trimmed.endsWith("/") ? trimmed : trimmed + "/");
  }
}
