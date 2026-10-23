package com.factory.expenses.ui.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/** Formats amounts with the installation currency reported by the metadata endpoint. */
public final class MoneyFormatter {

  private MoneyFormatter() {}

  public static String format(BigDecimal amount, String currencyCode) {
    return format(amount, currencyCode, Locale.getDefault());
  }

  static String format(BigDecimal amount, String currencyCode, Locale locale) {
    NumberFormat format = NumberFormat.getCurrencyInstance(locale);
    try {
      format.setCurrency(Currency.getInstance(currencyCode));
    } catch (IllegalArgumentException | NullPointerException e) {
      return amount.toPlainString();
    }
    return format.format(amount);
  }
}
