package com.factory.expenses.ui.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import com.factory.expenses.testing.Fixtures;
import com.factory.expenses.testing.RepoAnswers;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ListViewModelTest {

  @Rule public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

  private ExpenseRepository repository;
  private ListViewModel viewModel;
  private final List<ExpenseDto> expenses =
      Collections.singletonList(Fixtures.expense(1, "12.50", "FOOD", "2026-10-21"));

  @Before
  public void setUp() {
    repository = mock(ExpenseRepository.class);
    doAnswer(RepoAnswers.success(0, Fixtures.metadata())).when(repository).getMetadata(any());
    doAnswer(RepoAnswers.success(2, expenses)).when(repository).listExpenses(any(), any(), any());
    viewModel = new ListViewModel(repository, Fixtures.CLOCK);
  }

  @Test
  public void startsOnCurrentMonthWithAllCategories() {
    assertEquals(YearMonth.of(2026, 10), viewModel.getMonth().getValue());
    assertNull(viewModel.getCategory().getValue());
    assertEquals(ListViewModel.Status.LOADING, viewModel.getState().getValue().status);
  }

  @Test
  public void refreshLoadsCurrentMonthWithoutCategoryFilter() {
    viewModel.refresh();

    verify(repository).listExpenses(eq(YearMonth.of(2026, 10)), eq(null), any());
    assertEquals(ListViewModel.Status.CONTENT, viewModel.getState().getValue().status);
    assertEquals(expenses, viewModel.getState().getValue().expenses);
    assertEquals("USD", viewModel.getMetadata().getValue().currency);
  }

  @Test
  public void emptyResultIsExposedAsEmptyState() {
    doAnswer(RepoAnswers.success(2, Collections.<ExpenseDto>emptyList()))
        .when(repository)
        .listExpenses(any(), any(), any());

    viewModel.refresh();

    assertEquals(ListViewModel.Status.EMPTY, viewModel.getState().getValue().status);
  }

  @Test
  public void previousMonthReloadsWithEarlierMonth() {
    viewModel.previousMonth();

    assertEquals(YearMonth.of(2026, 9), viewModel.getMonth().getValue());
    verify(repository).listExpenses(eq(YearMonth.of(2026, 9)), eq(null), any());
  }

  @Test
  public void nextMonthIsCappedAtCurrentMonth() {
    viewModel.nextMonth();

    assertEquals(YearMonth.of(2026, 10), viewModel.getMonth().getValue());
    verify(repository, never()).listExpenses(any(), any(), any());
  }

  @Test
  public void nextMonthAfterPreviousReturnsToCurrentMonth() {
    viewModel.previousMonth();
    viewModel.nextMonth();

    assertEquals(YearMonth.of(2026, 10), viewModel.getMonth().getValue());
    verify(repository, times(1)).listExpenses(eq(YearMonth.of(2026, 10)), eq(null), any());
  }

  @Test
  public void categoryChangeReloadsWithCategoryAndSameMonth() {
    viewModel.setCategory("FOOD");

    assertEquals("FOOD", viewModel.getCategory().getValue());
    verify(repository).listExpenses(eq(YearMonth.of(2026, 10)), eq("FOOD"), any());
  }

  @Test
  public void selectingSameCategoryDoesNotReload() {
    viewModel.setCategory(null);

    verify(repository, never()).listExpenses(any(), any(), any());
  }

  @Test
  public void failureShowsErrorAndRetryReissuesRequest() {
    ApiError error = ApiError.network("down");
    doAnswer(RepoAnswers.failure(2, error)).when(repository).listExpenses(any(), any(), any());
    viewModel.refresh();
    assertEquals(ListViewModel.Status.ERROR, viewModel.getState().getValue().status);
    assertEquals(error, viewModel.getState().getValue().error);

    doAnswer(RepoAnswers.success(2, expenses)).when(repository).listExpenses(any(), any(), any());
    viewModel.retry();

    assertEquals(ListViewModel.Status.CONTENT, viewModel.getState().getValue().status);
    verify(repository, times(2)).listExpenses(any(), any(), any());
  }

  @Test
  public void metadataFailureShowsError() {
    doAnswer(RepoAnswers.failure(0, ApiError.network("down"))).when(repository).getMetadata(any());

    viewModel.refresh();

    assertEquals(ListViewModel.Status.ERROR, viewModel.getState().getValue().status);
    verify(repository, never()).listExpenses(any(), any(), any());
  }

  @Test
  public void staleResponseIsIgnored() {
    RepoAnswers.Pending<List<ExpenseDto>> first = new RepoAnswers.Pending<>(2);
    doAnswer(first).when(repository).listExpenses(any(), any(), any());
    viewModel.refresh();
    doAnswer(RepoAnswers.success(2, Collections.<ExpenseDto>emptyList()))
        .when(repository)
        .listExpenses(any(), any(), any());
    viewModel.setCategory("FOOD");

    first.callback().onSuccess(expenses);

    assertEquals(ListViewModel.Status.EMPTY, viewModel.getState().getValue().status);
  }

  @Test
  public void refreshKeepsContentVisibleWhileReloading() {
    viewModel.refresh();
    RepoAnswers.Pending<List<ExpenseDto>> pending = new RepoAnswers.Pending<>(2);
    doAnswer(pending).when(repository).listExpenses(any(), any(), any());

    viewModel.refresh();

    assertEquals(ListViewModel.Status.CONTENT, viewModel.getState().getValue().status);
  }

  @Test
  public void metadataIsAvailableForDisplayNames() {
    viewModel.refresh();

    MetadataDto metadata = viewModel.getMetadata().getValue();
    assertEquals("Food", metadata.displayName("FOOD"));
  }
}
