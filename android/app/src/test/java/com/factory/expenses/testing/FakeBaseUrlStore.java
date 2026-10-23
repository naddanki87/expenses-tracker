package com.factory.expenses.testing;

import com.factory.expenses.data.repository.BaseUrlStore;

/** In-memory {@link BaseUrlStore}. */
public final class FakeBaseUrlStore implements BaseUrlStore {

  private String baseUrl;

  public FakeBaseUrlStore(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public String getBaseUrl() {
    return baseUrl;
  }

  @Override
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
