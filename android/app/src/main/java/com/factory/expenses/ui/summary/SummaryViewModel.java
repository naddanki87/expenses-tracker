package com.factory.expenses.ui.summary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import java.time.Clock;
import java.time.YearMonth;

/** Loads the monthly total and per-category totals for the selected month. */
public class SummaryViewModel extends ViewModel {

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
    public final MonthlySummaryDto summary;
    public final ApiError error;

    private State(Status status, MonthlySummaryDto summary, ApiError error) {
      this.status = status;
      this.summary = summary;
      this.error = error;
    }
  }

  private final ExpenseRepository repository;
  private final Clock clock;
  private final MutableLiveData<State> state =
      new MutableLiveData<>(new State(Status.LOADING, null, null));
  private final MutableLiveData<YearMonth> month;
  private final MutableLiveData<MetadataDto> metadata = new MutableLiveData<>();
  private int requestId;

  public SummaryViewModel(ExpenseRepository repository, Clock clock) {
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

  public LiveData<MetadataDto> getMetadata() {
    return metadata;
  }

  /** Loads the summary; keeps showing the previous result while it refreshes. */
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

  /** Moves to the next month, capped at the current month. */
  public void nextMonth() {
    YearMonth selected = month.getValue();
    if (!selected.isBefore(YearMonth.now(clock))) {
      return;
    }
    month.setValue(selected.plusMonths(1));
    load(true);
  }

  private void load(boolean showLoading) {
    int id = ++requestId;
    YearMonth requestedMonth = month.getValue();
    if (showLoading) {
      state.setValue(new State(Status.LOADING, null, null));
    }
    repository.getMetadata(
        new ExpenseRepository.Callback<MetadataDto>() {
          @Override
          public void onSuccess(MetadataDto value) {
            if (id != requestId) {
              return;
            }
            metadata.setValue(value);
            loadSummary(id, requestedMonth);
          }

          @Override
          public void onError(ApiError error) {
            fail(id, error);
          }
        });
  }

  private void loadSummary(int id, YearMonth requestedMonth) {
    repository.getMonthlySummary(
        requestedMonth,
        new ExpenseRepository.Callback<MonthlySummaryDto>() {
          @Override
          public void onSuccess(MonthlySummaryDto value) {
            if (id != requestId) {
              return;
            }
            Status status = value.count == 0 ? Status.EMPTY : Status.CONTENT;
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
      state.setValue(new State(Status.ERROR, null, error));
    }
  }
}
