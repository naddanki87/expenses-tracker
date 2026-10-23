package com.factory.expenses.api;

import java.time.YearMonth;
import java.util.regex.Pattern;

final class MonthParser {

  private static final Pattern FORMAT = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

  private MonthParser() {}

  static YearMonth parse(String value) {
    if (value == null || !FORMAT.matcher(value).matches()) {
      throw new InvalidParameterException("month", "must be a month in the form YYYY-MM");
    }
    return YearMonth.parse(value);
  }
}
