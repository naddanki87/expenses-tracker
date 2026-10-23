package com.factory.expenses.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;
import org.junit.Test;

public class BaseUrlValidatorTest {

  @Test
  public void acceptsLanAddressAndAddsTrailingSlash() {
    assertEquals(
        Optional.of("http://192.168.1.10:8080/"),
        BaseUrlValidator.normalize("http://192.168.1.10:8080"));
  }

  @Test
  public void keepsExistingTrailingSlash() {
    assertEquals(Optional.of("https://host/"), BaseUrlValidator.normalize("https://host/"));
  }

  @Test
  public void trimsWhitespace() {
    assertEquals(Optional.of("http://host/"), BaseUrlValidator.normalize("  http://host  "));
  }

  @Test
  public void rejectsUnsupportedSchemeFreeTextEmptyAndNull() {
    assertFalse(BaseUrlValidator.normalize("ftp://x").isPresent());
    assertFalse(BaseUrlValidator.normalize("not a url").isPresent());
    assertFalse(BaseUrlValidator.normalize("").isPresent());
    assertFalse(BaseUrlValidator.normalize("   ").isPresent());
    assertFalse(BaseUrlValidator.normalize(null).isPresent());
  }
}
