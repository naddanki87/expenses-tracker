package com.factory.expenses.ui.summary;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.factory.expenses.data.api.dto.CategoryTotalDto;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import com.factory.expenses.testing.Fixtures;
import com.factory.expenses.testing.RepoAnswers;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SummaryViewModelTest {

  @Rule public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

  private ExpenseRepository repository;
  private SummaryViewModel viewModel;

  private static MonthlySummaryDto october() {
    return new MonthlySummaryDto(
        YearMonth.of(2026, 10),
        "USD",
        new BigDecimal("245.50"),
        7,
        Arrays.asList(
            new CategoryTotalDto("HOUSING", new BigDecimal("150.00"), 1),
            new CategoryTotalDto("FOOD", new BigDecimal("60.50"), 4)));
  }

  @Before
  public void setUp() {
    repository = mock(ExpenseRepository.class);
    doAnswer(RepoAnswers.success(0, Fixtures.metadata())).when(repository).getMetadata(any());
    doAnswer(RepoAnswers.success(1, october())).when(repository).getMonthlySummary(any(), any());
    viewModel = new SummaryViewModel(repository, Fixtures.CLOCK);
  }

  @Test
  public void loadsSummaryOfCurrentMonthInServerOrder() {
    viewModel.refresh();

    verify(repository).getMonthlySummary(eq(YearMonth.of(2026, 10)), any());
    SummaryViewModel.State state = viewModel.getState().getValue();
    assertEquals(SummaryViewModel.Status.CONTENT, state.status);
    assertEquals(new BigDecimal("245.50"), state.summary.total);
    assertEquals(7L, state.summary.count);
    assertEquals("HOUSING", state.summary.categories.get(0).category);
    assertEquals("FOOD", state.summary.categories.get(1).category);
    assertEquals("Food", viewModel.getMetadata().getValue().displayName("FOOD"));
  }

  @Test
  public void emptyMonthIsEmptyStateWithZeroTotal() {
    MonthlySummaryDto empty =
        new MonthlySummaryDto(
            YearMonth.of(2026, 9),
            "USD",
            new BigDecimal("0.00"),
            0,
            Collections.<CategoryTotalDto>emptyList());
    doAnswer(RepoAnswers.success(1, empty)).when(repository).getMonthlySummary(any(), any());

    viewModel.previousMonth();

    verify(repository).getMonthlySummary(eq(YearMonth.of(2026, 9)), any());
    SummaryViewModel.State state = viewModel.getState().getValue();
    assertEquals(SummaryViewModel.Status.EMPTY, state.status);
    assertEquals("0.00", state.summary.total.toString());
  }

  @Test
  public void nextMonthIsCappedAtCurrentMonth() {
    viewModel.nextMonth();

    assertEquals(YearMonth.of(2026, 10), viewModel.getMonth().getValue());
    verify(repository, never()).getMonthlySummary(any(), any());
  }

  @Test
  public void nextMonthMovesForwardAfterPrevious() {
    viewModel.previousMonth();
    viewModel.nextMonth();

    assertEquals(YearMonth.of(2026, 10), viewModel.getMonth().getValue());
    verify(repository, times(1)).getMonthlySummary(eq(YearMonth.of(2026, 10)), any());
  }

  @Test
  public void failureThenRetry() {
    ApiError error = ApiError.network("down");
    doAnswer(RepoAnswers.failure(1, error)).when(repository).getMonthlySummary(any(), any());
    viewModel.refresh();
    assertEquals(SummaryViewModel.Status.ERROR, viewModel.getState().getValue().status);
    assertEquals(error, viewModel.getState().getValue().error);

    doAnswer(RepoAnswers.success(1, october())).when(repository).getMonthlySummary(any(), any());
    viewModel.retry();

    assertEquals(SummaryViewModel.Status.CONTENT, viewModel.getState().getValue().status);
  }

  @Test
  public void metadataFailureShowsError() {
    doAnswer(RepoAnswers.failure(0, ApiError.network("down"))).when(repository).getMetadata(any());

    viewModel.refresh();

    assertEquals(SummaryViewModel.Status.ERROR, viewModel.getState().getValue().status);
  }

  @Test
  public void staleResponseIsIgnored() {
    RepoAnswers.Pending<MonthlySummaryDto> first = new RepoAnswers.Pending<>(1);
    doAnswer(first).when(repository).getMonthlySummary(any(), any());
    viewModel.refresh();
    doAnswer(RepoAnswers.failure(1, ApiError.network("down")))
        .when(repository)
        .getMonthlySummary(any(), any());
    viewModel.previousMonth();

    first.callback().onSuccess(october());

    assertEquals(SummaryViewModel.Status.ERROR, viewModel.getState().getValue().status);
  }

  @Test
  public void refreshKeepsContentVisibleWhileReloading() {
    viewModel.refresh();
    doAnswer(new RepoAnswers.Pending<MonthlySummaryDto>(1))
        .when(repository)
        .getMonthlySummary(any(), any());

    viewModel.refresh();

    assertEquals(SummaryViewModel.Status.CONTENT, viewModel.getState().getValue().status);
  }
}
