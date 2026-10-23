package com.factory.expenses.ui.list;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

/** Holds the month and category filters and the list they produce. */
public class ListViewModel extends ViewModel {

  /** What the screen should show. */
  public enum Status {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR
  }

  /** Immutable snapshot of the screen state. */
  public static final class State {
    public final Status status;
    public final List<ExpenseDto> expenses;
    public final ApiError error;

    private State(Status status, List<ExpenseDto> expenses, ApiError error) {
      this.status = status;
      this.expenses = expenses;
      this.error = error;
    }
  }

  private final ExpenseRepository repository;
  private final Clock clock;
  private final MutableLiveData<State> state =
      new MutableLiveData<>(new State(Status.LOADING, Collections.emptyList(), null));
  private final MutableLiveData<YearMonth> month;
  private final MutableLiveData<String> category = new MutableLiveData<>(null);
  private final MutableLiveData<MetadataDto> metadata = new MutableLiveData<>();
  private int requestId;

  public ListViewModel(ExpenseRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
    this.month = new MutableLiveData<>(YearMonth.now(clock));
  }

  public LiveData<State> getState() {
    return state;
  }

  public LiveData<YearMonth> getMonth() {
    return month;
  }

  /** Selected category code, or null for all categories. */
  public LiveData<String> getCategory() {
    return category;
  }

  public LiveData<MetadataDto> getMetadata() {
    return metadata;
  }

  /** Loads the list; keeps showing the previous result while it refreshes. */
  public void refresh() {
    Status current = state.getValue().status;
    load(current != Status.CONTENT && current != Status.EMPTY);
  }

  public void retry() {
    load(true);
  }

  public void previousMonth() {
    month.setValue(month.getValue().minusMonths(1));
    load(true);
  }

  /** Moves to the next month; expenses cannot be in the future, so the current month is the cap. */
  public void nextMonth() {
    YearMonth selected = month.getValue();
    if (!selected.isBefore(YearMonth.now(clock))) {
      return;
    }
    month.setValue(selected.plusMonths(1));
    load(true);
  }

  public void setCategory(String code) {
    String current = category.getValue();
    if (current == null ? code == null : current.equals(code)) {
      return;
    }
    category.setValue(code);
    load(true);
  }

  private void load(boolean showLoading) {
    int id = ++requestId;
    YearMonth requestedMonth = month.getValue();
    String requestedCategory = category.getValue();
    if (showLoading) {
      state.setValue(new State(Status.LOADING, Collections.emptyList(), null));
    }
    repository.getMetadata(
        new ExpenseRepository.Callback<MetadataDto>() {
          @Override
          public void onSuccess(MetadataDto value) {
            if (id != requestId) {
              return;
            }
            metadata.setValue(value);
            loadExpenses(id, requestedMonth, requestedCategory);
          }

          @Override
          public void onError(ApiError error) {
            fail(id, error);
          }
        });
  }

  private void loadExpenses(int id, YearMonth requestedMonth, String requestedCategory) {
    repository.listExpenses(
        requestedMonth,
        requestedCategory,
        new ExpenseRepository.Callback<List<ExpenseDto>>() {
          @Override
          public void onSuccess(List<ExpenseDto> value) {
            if (id != requestId) {
              return;
            }
            Status status = value.isEmpty() ? Status.EMPTY : Status.CONTENT;
            state.setValue(new State(status, value, null));
          }

          @Override
          public void onError(ApiError error) {
            fail(id, error);
          }
        });
  }

  private void fail(int id, ApiError error) {
    if (id == requestId) {
      state.setValue(new State(Status.ERROR, Collections.emptyList(), error));
    }
  }
}
