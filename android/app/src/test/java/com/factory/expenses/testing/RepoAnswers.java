package com.factory.expenses.testing;

import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import org.mockito.stubbing.Answer;

/** Mockito answers that complete the callback argument of a mocked repository call. */
public final class RepoAnswers {

  private RepoAnswers() {}

  @SuppressWarnings("unchecked")
  public static <T> Answer<Void> success(int callbackIndex, T value) {
    return invocation -> {
      ((ExpenseRepository.Callback<T>) invocation.getArgument(callbackIndex)).onSuccess(value);
      return null;
    };
  }

  @SuppressWarnings("unchecked")
  public static Answer<Void> failure(int callbackIndex, ApiError error) {
    return invocation -> {
      ((ExpenseRepository.Callback<Object>) invocation.getArgument(callbackIndex)).onError(error);
      return null;
    };
  }

  /** Captures the callback without completing it, to test races and pending states. */
  public static final class Pending<T> implements Answer<Void> {
    private final int callbackIndex;
    private ExpenseRepository.Callback<T> callback;

    public Pending(int callbackIndex) {
      this.callbackIndex = callbackIndex;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void answer(org.mockito.invocation.InvocationOnMock invocation) {
      callback = invocation.getArgument(callbackIndex);
      return null;
    }

    public ExpenseRepository.Callback<T> callback() {
      return callback;
    }
  }
}
