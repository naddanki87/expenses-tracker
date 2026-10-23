package com.factory.expenses.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;
import org.junit.Test;

public class MoneyFormatterTest {

  @Test
  public void formatsWithCurrencySymbolAndTwoDecimals() {
    assertEquals("$12.50", MoneyFormatter.format(new BigDecimal("12.5"), "USD", Locale.US));
  }

  @Test
  public void unknownCurrencyFallsBackToPlainNumber() {
    assertEquals("12.50", MoneyFormatter.format(new BigDecimal("12.50"), "??", Locale.US));
    assertEquals("12.50", MoneyFormatter.format(new BigDecimal("12.50"), null, Locale.US));
  }

  @Test
  public void eventIsDeliveredOnce() {
    Event<String> event = new Event<>("x");

    assertTrue(event.getContentIfNotHandled().isPresent());
    assertFalse(event.getContentIfNotHandled().isPresent());
    assertEquals("x", event.peekContent());
  }
}
