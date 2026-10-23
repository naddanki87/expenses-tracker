package com.factory.expenses.ui.common;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.factory.expenses.R;
import com.factory.expenses.app.ExpensesApp;
import com.factory.expenses.data.repository.ApiError;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Small helpers shared by the fragments. */
public final class UiSupport {

  private UiSupport() {}

  public static ViewModelProvider provider(Fragment fragment) {
    ExpensesApp app = (ExpensesApp) fragment.requireActivity().getApplication();
    return new ViewModelProvider(fragment, app.container().viewModelFactory());
  }

  @StringRes
  public static int messageFor(ApiError.Kind kind) {
    switch (kind) {
      case NETWORK:
        return R.string.error_network;
      case NOT_FOUND:
        return R.string.already_deleted;
      case SERVER:
        return R.string.error_server;
      default:
        return R.string.error_generic;
    }
  }

  public static String monthLabel(YearMonth month) {
    return month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
        + " "
        + month.getYear();
  }
}
