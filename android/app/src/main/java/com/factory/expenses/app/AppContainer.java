package com.factory.expenses.app;

import android.content.Context;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.factory.expenses.BuildConfig;
import com.factory.expenses.data.repository.BaseUrlStore;
import com.factory.expenses.data.repository.ExpenseRepository;
import com.factory.expenses.ui.edit.EditViewModel;
import com.factory.expenses.ui.list.ListViewModel;
import com.factory.expenses.ui.settings.SettingsViewModel;
import com.factory.expenses.ui.summary.SummaryViewModel;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/** Hand-wired dependencies shared by all screens. */
public final class AppContainer {

  private final BaseUrlStore baseUrlStore;
  private final ExpenseRepository repository;
  private final Clock clock = Clock.systemDefaultZone();

  public AppContainer(Context context) {
    this.baseUrlStore = new SharedPreferencesBaseUrlStore(context, BuildConfig.API_BASE_URL);
    OkHttpClient client =
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    this.repository = new ExpenseRepository(baseUrlStore, client);
  }

  public ViewModelProvider.Factory viewModelFactory() {
    return new ViewModelProvider.Factory() {
      @Override
      @SuppressWarnings("unchecked")
      public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ListViewModel.class)) {
          return (T) new ListViewModel(repository, clock);
        }
        if (modelClass.isAssignableFrom(EditViewModel.class)) {
          return (T) new EditViewModel(repository, clock);
        }
        if (modelClass.isAssignableFrom(SummaryViewModel.class)) {
          return (T) new SummaryViewModel(repository, clock);
        }
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
          return (T) new SettingsViewModel(baseUrlStore);
        }
        throw new IllegalArgumentException("Unknown ViewModel " + modelClass.getName());
      }
    };
  }
}
