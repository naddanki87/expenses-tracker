package com.factory.expenses.data.repository;

/** Persistence of the API base URL chosen in Settings. */
public interface BaseUrlStore {

  String getBaseUrl();

  void setBaseUrl(String baseUrl);
}
