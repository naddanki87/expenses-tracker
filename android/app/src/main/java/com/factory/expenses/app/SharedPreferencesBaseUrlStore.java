package com.factory.expenses.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.factory.expenses.data.repository.BaseUrlStore;

/** Keeps the API base URL in SharedPreferences, defaulting to the build-time value. */
public final class SharedPreferencesBaseUrlStore implements BaseUrlStore {

  private static final String PREFS_NAME = "settings";
  private static final String KEY_BASE_URL = "base_url";

  private final SharedPreferences preferences;
  private final String defaultUrl;

  public SharedPreferencesBaseUrlStore(Context context, String defaultUrl) {
    this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    this.defaultUrl = defaultUrl;
  }

  @Override
  public String getBaseUrl() {
    return preferences.getString(KEY_BASE_URL, defaultUrl);
  }

  @Override
  public void setBaseUrl(String baseUrl) {
    preferences.edit().putString(KEY_BASE_URL, baseUrl).apply();
  }
}
