package com.factory.expenses.ui.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.ExpenseRequestDto;
import com.factory.expenses.data.api.dto.FieldErrorDto;
import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import com.factory.expenses.testing.Fixtures;
import com.factory.expenses.testing.RepoAnswers;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class EditViewModelTest {

  @Rule public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

  private ExpenseRepository repository;
  private EditViewModel viewModel;
  private final ExpenseDto existing = Fixtures.expense(42, "12.50", "FOOD", "2026-10-21");

  @Before
  public void setUp() {
    repository = mock(ExpenseRepository.class);
    doAnswer(RepoAnswers.success(0, Fixtures.metadata())).when(repository).getMetadata(any());
    doAnswer(RepoAnswers.success(1, existing)).when(repository).createExpense(any(), any());
    doAnswer(RepoAnswers.success(2, existing))
        .when(repository)
        .updateExpense(anyLong(), any(), any());
    doAnswer(RepoAnswers.success(1, existing)).when(repository).getExpense(anyLong(), any());
    doAnswer(RepoAnswers.success(1, (Void) null)).when(repository).deleteExpense(anyLong(), any());
    viewModel = new EditViewModel(repository, Fixtures.CLOCK);
  }

  private void startNew() {
    viewModel.start(EditViewModel.NEW_EXPENSE);
    viewModel.setCategory("FOOD");
  }

  private EditViewModel.Reason errorOn(EditViewModel.Field field) {
    EditViewModel.FormError error = viewModel.getFieldErrors().getValue().get(field);
    return error == null ? null : error.reason;
  }

  private ExpenseRequestDto savedRequest() {
    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);
    verify(repository).createExpense(captor.capture(), any());
    return captor.getValue();
  }

  @Test
  public void newExpenseIsReadyAfterMetadataAndDefaultsToToday() {
    viewModel.start(EditViewModel.NEW_EXPENSE);

    assertEquals(EditViewModel.LoadState.READY, viewModel.getLoadState().getValue());
    assertFalse(viewModel.isEditing());
    assertEquals(LocalDate.of(2026, 10, 21), viewModel.getSelectedDate().getValue());
    assertEquals(LocalDate.of(2026, 10, 21), viewModel.today());
    assertEquals("USD", viewModel.getMetadata().getValue().currency);
  }

  @Test
  public void metadataFailureShowsErrorAndRetryRecovers() {
    doAnswer(RepoAnswers.failure(0, ApiError.network("down"))).when(repository).getMetadata(any());
    viewModel.start(EditViewModel.NEW_EXPENSE);
    assertEquals(EditViewModel.LoadState.ERROR, viewModel.getLoadState().getValue());
    assertEquals(ApiError.Kind.NETWORK, viewModel.getLoadError().getValue().getKind());

    doAnswer(RepoAnswers.success(0, Fixtures.metadata())).when(repository).getMetadata(any());
    viewModel.retry();

    assertEquals(EditViewModel.LoadState.READY, viewModel.getLoadState().getValue());
  }

  @Test
  public void startIsIdempotent() {
    viewModel.start(EditViewModel.NEW_EXPENSE);
    viewModel.start(42);

    assertFalse(viewModel.isEditing());
  }

  @Test
  public void editingLoadsExpenseAndPrefillsForm() {
    viewModel.start(42);

    assertTrue(viewModel.isEditing());
    assertEquals(EditViewModel.LoadState.READY, viewModel.getLoadState().getValue());
    assertEquals("FOOD", viewModel.getSelectedCategory().getValue());
    assertEquals(LocalDate.of(2026, 10, 21), viewModel.getSelectedDate().getValue());
    assertEquals(existing, viewModel.getPrefill().getValue().peekContent());
  }

  @Test
  public void editingMissingExpenseTellsUserAndGoesBack() {
    doAnswer(RepoAnswers.failure(1, new ApiError(ApiError.Kind.NOT_FOUND, 404, null, null, null)))
        .when(repository)
        .getExpense(anyLong(), any());

    viewModel.start(42);

    assertEquals(ApiError.Kind.NOT_FOUND, viewModel.getNotice().getValue().peekContent());
    assertTrue(viewModel.getNavigateBack().getValue().peekContent());
  }

  @Test
  public void editingLoadFailureShowsRetryableError() {
    doAnswer(RepoAnswers.failure(1, ApiError.network("down")))
        .when(repository)
        .getExpense(anyLong(), any());

    viewModel.start(42);

    assertEquals(EditViewModel.LoadState.ERROR, viewModel.getLoadState().getValue());
  }

  @Test
  public void validNewExpensePostsAndNavigatesBack() {
    startNew();

    viewModel.save("45", "  lunch  ");

    ExpenseRequestDto request = savedRequest();
    assertEquals("45.00", request.amount.toString());
    assertEquals("FOOD", request.category);
    assertEquals(LocalDate.of(2026, 10, 21), request.date);
    assertEquals("lunch", request.note);
    assertTrue(viewModel.getNavigateBack().getValue().peekContent());
    assertFalse(viewModel.getSaving().getValue());
  }

  @Test
  public void blankNoteIsSentAsNull() {
    startNew();

    viewModel.save("1.5", "   ");

    assertNull(savedRequest().note);
  }

  @Test
  public void editingExistingExpenseSendsPutWithAllFields() {
    viewModel.start(42);
    viewModel.setDate(LocalDate.of(2026, 10, 20));

    viewModel.save("14.00", "tip");

    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);
    verify(repository).updateExpense(org.mockito.ArgumentMatchers.eq(42L), captor.capture(), any());
    verify(repository, never()).createExpense(any(), any());
    assertEquals("14.00", captor.getValue().amount.toString());
    assertEquals("FOOD", captor.getValue().category);
    assertEquals(LocalDate.of(2026, 10, 20), captor.getValue().date);
    assertEquals("tip", captor.getValue().note);
    assertTrue(viewModel.getNavigateBack().getValue().peekContent());
  }

  @Test
  public void emptyAndUnparsableAmountAreRequired() {
    startNew();

    viewModel.save("", "");
    assertEquals(EditViewModel.Reason.AMOUNT_REQUIRED, errorOn(EditViewModel.Field.AMOUNT));
    viewModel.save("abc", "");
    assertEquals(EditViewModel.Reason.AMOUNT_REQUIRED, errorOn(EditViewModel.Field.AMOUNT));
    viewModel.save(null, "");
    assertEquals(EditViewModel.Reason.AMOUNT_REQUIRED, errorOn(EditViewModel.Field.AMOUNT));
    verify(repository, never()).createExpense(any(), any());
  }

  @Test
  public void zeroAndNegativeAmountsAreRejected() {
    startNew();

    viewModel.save("0", "");
    assertEquals(EditViewModel.Reason.AMOUNT_NOT_POSITIVE, errorOn(EditViewModel.Field.AMOUNT));
    viewModel.save("-3", "");
    assertEquals(EditViewModel.Reason.AMOUNT_NOT_POSITIVE, errorOn(EditViewModel.Field.AMOUNT));
  }

  @Test
  public void moreThanTwoDecimalsIsRejectedButTrailingZerosAreFine() {
    startNew();

    viewModel.save("1.234", "");
    assertEquals(
        EditViewModel.Reason.AMOUNT_TOO_MANY_DECIMALS, errorOn(EditViewModel.Field.AMOUNT));
    viewModel.save("1.500", "");
    assertNull(errorOn(EditViewModel.Field.AMOUNT));
    assertEquals("1.50", savedRequest().amount.toString());
  }

  @Test
  public void tooLargeAmountIsRejected() {
    startNew();

    viewModel.save("10000000000", "");

    assertEquals(EditViewModel.Reason.AMOUNT_TOO_LARGE, errorOn(EditViewModel.Field.AMOUNT));
  }

  @Test
  public void maximumAmountIsAccepted() {
    startNew();

    viewModel.save("9999999999.99", "");

    assertEquals("9999999999.99", savedRequest().amount.toString());
  }

  @Test
  public void missingCategoryIsRejected() {
    viewModel.start(EditViewModel.NEW_EXPENSE);

    viewModel.save("5", "");

    assertEquals(EditViewModel.Reason.CATEGORY_REQUIRED, errorOn(EditViewModel.Field.CATEGORY));
  }

  @Test
  public void futureAndMissingDatesAreRejectedButTodayIsAccepted() {
    startNew();

    viewModel.setDate(LocalDate.of(2026, 10, 22));
    viewModel.save("5", "");
    assertEquals(EditViewModel.Reason.DATE_IN_FUTURE, errorOn(EditViewModel.Field.DATE));

    viewModel.setDate(null);
    viewModel.save("5", "");
    assertEquals(EditViewModel.Reason.DATE_REQUIRED, errorOn(EditViewModel.Field.DATE));

    viewModel.setDate(LocalDate.of(2026, 10, 21));
    viewModel.save("5", "");
    assertNull(errorOn(EditViewModel.Field.DATE));
  }

  @Test
  public void noteLongerThan500CharactersIsRejected() {
    startNew();

    viewModel.save("5", new String(new char[501]).replace('\0', 'a'));
    assertEquals(EditViewModel.Reason.NOTE_TOO_LONG, errorOn(EditViewModel.Field.NOTE));

    viewModel.save("5", new String(new char[500]).replace('\0', 'a'));
    assertNull(errorOn(EditViewModel.Field.NOTE));
  }

  @Test
  public void severalInvalidFieldsAreReportedTogether() {
    viewModel.start(EditViewModel.NEW_EXPENSE);

    viewModel.save("", "");

    assertEquals(EditViewModel.Reason.AMOUNT_REQUIRED, errorOn(EditViewModel.Field.AMOUNT));
    assertEquals(EditViewModel.Reason.CATEGORY_REQUIRED, errorOn(EditViewModel.Field.CATEGORY));
  }

  @Test
  public void serverFieldErrorsAreMappedOntoForm() {
    startNew();
    ApiError error =
        new ApiError(
            ApiError.Kind.VALIDATION,
            400,
            "Validation failed",
            null,
            java.util.Arrays.asList(
                new FieldErrorDto("amount", "must be greater than 0"),
                new FieldErrorDto("date", "must not be in the future")));
    doAnswer(RepoAnswers.failure(1, error)).when(repository).createExpense(any(), any());

    viewModel.save("5", "");

    EditViewModel.FormError amount =
        viewModel.getFieldErrors().getValue().get(EditViewModel.Field.AMOUNT);
    assertEquals(EditViewModel.Reason.SERVER, amount.reason);
    assertEquals("must be greater than 0", amount.message);
    assertEquals(EditViewModel.Reason.SERVER, errorOn(EditViewModel.Field.DATE));
    assertNull(viewModel.getNavigateBack().getValue());
    assertFalse(viewModel.getSaving().getValue());
  }

  @Test
  public void validationErrorWithoutKnownFieldBecomesNotice() {
    startNew();
    ApiError error =
        new ApiError(
            ApiError.Kind.VALIDATION, 400, null, null, Collections.<FieldErrorDto>emptyList());
    doAnswer(RepoAnswers.failure(1, error)).when(repository).createExpense(any(), any());

    viewModel.save("5", "");

    assertEquals(ApiError.Kind.VALIDATION, viewModel.getNotice().getValue().peekContent());
  }

  @Test
  public void networkFailureOnSaveIsReportedAndStaysOnForm() {
    startNew();
    doAnswer(RepoAnswers.failure(1, ApiError.network("down")))
        .when(repository)
        .createExpense(any(), any());

    viewModel.save("5", "");

    assertEquals(ApiError.Kind.NETWORK, viewModel.getNotice().getValue().peekContent());
    assertNull(viewModel.getNavigateBack().getValue());
  }

  @Test
  public void updatingDeletedExpenseSaysAlreadyDeletedAndGoesBack() {
    viewModel.start(42);
    doAnswer(RepoAnswers.failure(2, new ApiError(ApiError.Kind.NOT_FOUND, 404, null, null, null)))
        .when(repository)
        .updateExpense(anyLong(), any(), any());

    viewModel.save("5", "");

    assertEquals(ApiError.Kind.NOT_FOUND, viewModel.getNotice().getValue().peekContent());
    assertTrue(viewModel.getNavigateBack().getValue().peekContent());
  }

  @Test
  public void secondSaveWhileSavingIsIgnored() {
    startNew();
    RepoAnswers.Pending<ExpenseDto> pending = new RepoAnswers.Pending<>(1);
    doAnswer(pending).when(repository).createExpense(any(), any());

    viewModel.save("5", "");
    viewModel.save("5", "");

    assertTrue(viewModel.getSaving().getValue());
    verify(repository).createExpense(any(), any());
  }

  @Test
  public void deleteCallsApiAndNavigatesBack() {
    viewModel.start(42);

    viewModel.delete();

    verify(repository).deleteExpense(org.mockito.ArgumentMatchers.eq(42L), any());
    assertTrue(viewModel.getNavigateBack().getValue().peekContent());
  }

  @Test
  public void deleteOfAlreadyDeletedExpenseSaysSoAndGoesBack() {
    viewModel.start(42);
    doAnswer(RepoAnswers.failure(1, new ApiError(ApiError.Kind.NOT_FOUND, 404, null, null, null)))
        .when(repository)
        .deleteExpense(anyLong(), any());

    viewModel.delete();

    assertEquals(ApiError.Kind.NOT_FOUND, viewModel.getNotice().getValue().peekContent());
    assertTrue(viewModel.getNavigateBack().getValue().peekContent());
  }

  @Test
  public void deleteFailureStaysOnScreen() {
    viewModel.start(42);
    doAnswer(RepoAnswers.failure(1, ApiError.network("down")))
        .when(repository)
        .deleteExpense(anyLong(), any());

    viewModel.delete();

    assertEquals(ApiError.Kind.NETWORK, viewModel.getNotice().getValue().peekContent());
    assertNull(viewModel.getNavigateBack().getValue());
    assertFalse(viewModel.getSaving().getValue());
  }

  @Test
  public void deleteIsIgnoredForNewExpense() {
    startNew();

    viewModel.delete();

    verify(repository, never()).deleteExpense(anyLong(), any());
  }
}
