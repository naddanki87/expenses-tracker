package com.factory.expenses.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.factory.expenses.data.repository.BaseUrlStore;
import com.factory.expenses.data.repository.BaseUrlValidator;
import com.factory.expenses.ui.common.Event;
import java.util.Optional;

/** Edits the API base URL. */
public class SettingsViewModel extends ViewModel {

  private final BaseUrlStore store;
  private final MutableLiveData<String> currentUrl;
  private final MutableLiveData<Boolean> urlInvalid = new MutableLiveData<>(false);
  private final MutableLiveData<Event<Boolean>> saved = new MutableLiveData<>();

  public SettingsViewModel(BaseUrlStore store) {
    this.store = store;
    this.currentUrl = new MutableLiveData<>(store.getBaseUrl());
  }

  public LiveData<String> getCurrentUrl() {
    return currentUrl;
  }

  public LiveData<Boolean> getUrlInvalid() {
    return urlInvalid;
  }

  public LiveData<Event<Boolean>> getSaved() {
    return saved;
  }

  /** Validates and persists the URL typed by the user. */
  public void save(String input) {
    Optional<String> normalized = BaseUrlValidator.normalize(input);
    if (!normalized.isPresent()) {
      urlInvalid.setValue(true);
      return;
    }
    urlInvalid.setValue(false);
    store.setBaseUrl(normalized.get());
    currentUrl.setValue(normalized.get());
    saved.setValue(new Event<>(true));
  }
}
