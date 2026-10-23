package com.factory.expenses.ui.edit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.ExpenseRequestDto;
import com.factory.expenses.data.api.dto.FieldErrorDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.repository.ApiError;
import com.factory.expenses.data.repository.ExpenseRepository;
import com.factory.expenses.ui.common.Event;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Form state, client-side validation and save/delete for one expense. */
public class EditViewModel extends ViewModel {

  static final int MAX_NOTE_LENGTH = 500;
  static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");
  static final long NEW_EXPENSE = 0L;

  /** Form inputs that can carry an error. */
  public enum Field {
    AMOUNT,
    CATEGORY,
    DATE,
    NOTE
  }

  /** Why a field is invalid; the UI maps each reason to a string resource. */
  public enum Reason {
    AMOUNT_REQUIRED,
    AMOUNT_NOT_POSITIVE,
    AMOUNT_TOO_MANY_DECIMALS,
    AMOUNT_TOO_LARGE,
    CATEGORY_REQUIRED,
    DATE_REQUIRED,
    DATE_IN_FUTURE,
    NOTE_TOO_LONG,
    SERVER
  }

  /** Error on one field. {@code message} is only set for {@link Reason#SERVER}. */
  public static final class FormError {
    public final Reason reason;
    public final String message;

    FormError(Reason reason, String message) {
      this.reason = reason;
      this.message = message;
    }
  }

  /** Loading of metadata and, when editing, of the expense. */
  public enum LoadState {
    LOADING,
    READY,
    ERROR
  }

  private final ExpenseRepository repository;
  private final Clock clock;
  private final MutableLiveData<LoadState> loadState = new MutableLiveData<>(LoadState.LOADING);
  private final MutableLiveData<ApiError> loadError = new MutableLiveData<>();
  private final MutableLiveData<MetadataDto> metadata = new MutableLiveData<>();
  private final MutableLiveData<Map<Field, FormError>> fieldErrors =
      new MutableLiveData<>(Collections.emptyMap());
  private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
  private final MutableLiveData<LocalDate> selectedDate;
  private final MutableLiveData<String> selectedCategory = new MutableLiveData<>();
  private final MutableLiveData<Event<ExpenseDto>> prefill = new MutableLiveData<>();
  private final MutableLiveData<Event<ApiError.Kind>> notice = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> navigateBack = new MutableLiveData<>();
  private long expenseId = NEW_EXPENSE;
  private boolean started;

  public EditViewModel(ExpenseRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
    this.selectedDate = new MutableLiveData<>(LocalDate.now(clock));
  }

  public LiveData<LoadState> getLoadState() {
    return loadState;
  }

  public LiveData<ApiError> getLoadError() {
    return loadError;
  }

  public LiveData<MetadataDto> getMetadata() {
    return metadata;
  }

  public LiveData<Map<Field, FormError>> getFieldErrors() {
    return fieldErrors;
  }

  public LiveData<Boolean> getSaving() {
    return saving;
  }

  public LiveData<LocalDate> getSelectedDate() {
    return selectedDate;
  }

  public LiveData<String> getSelectedCategory() {
    return selectedCategory;
  }

  /** Emitted once when an existing expense has been loaded, to fill the text inputs. */
  public LiveData<Event<ExpenseDto>> getPrefill() {
    return prefill;
  }

  /** Emitted for failures the user should be told about; NOT_FOUND means already deleted. */
  public LiveData<Event<ApiError.Kind>> getNotice() {
    return notice;
  }

  public LiveData<Event<Boolean>> getNavigateBack() {
    return navigateBack;
  }

  public boolean isEditing() {
    return expenseId != NEW_EXPENSE;
  }

  public LocalDate today() {
    return LocalDate.now(clock);
  }

  /** Starts loading once; {@code id} is {@link #NEW_EXPENSE} for a new expense. */
  public void start(long id) {
    if (started) {
      return;
    }
    started = true;
    expenseId = id;
    load();
  }

  public void retry() {
    load();
  }

  public void setCategory(String code) {
    selectedCategory.setValue(code);
  }

  public void setDate(LocalDate date) {
    selectedDate.setValue(date);
  }

  /** Validates the form and, when valid, creates or updates the expense. */
  public void save(String amountText, String noteText) {
    if (Boolean.TRUE.equals(saving.getValue())) {
      return;
    }
    Map<Field, FormError> errors = new EnumMap<>(Field.class);
    BigDecimal amount = parseAmount(amountText, errors);
    String category = selectedCategory.getValue();
    LocalDate date = selectedDate.getValue();
    validateCategory(category, errors);
    validateDate(date, errors);
    String note = noteText == null ? "" : noteText.trim();
    if (note.length() > MAX_NOTE_LENGTH) {
      errors.put(Field.NOTE, new FormError(Reason.NOTE_TOO_LONG, null));
    }
    fieldErrors.setValue(errors);
    if (!errors.isEmpty()) {
      return;
    }
    submit(
        new ExpenseRequestDto(
            amount.setScale(2, RoundingMode.UNNECESSARY),
            category,
            date,
            note.isEmpty() ? null : note));
  }

  /** Deletes the expense being edited; the UI asks for confirmation before calling this. */
  public void delete() {
    if (!isEditing() || Boolean.TRUE.equals(saving.getValue())) {
      return;
    }
    saving.setValue(true);
    repository.deleteExpense(
        expenseId,
        new ExpenseRepository.Callback<Void>() {
          @Override
          public void onSuccess(Void value) {
            saving.setValue(false);
            navigateBack.setValue(new Event<>(true));
          }

          @Override
          public void onError(ApiError error) {
            saving.setValue(false);
            notice.setValue(new Event<>(error.getKind()));
            if (error.getKind() == ApiError.Kind.NOT_FOUND) {
              navigateBack.setValue(new Event<>(true));
            }
          }
        });
  }

  private void load() {
    loadState.setValue(LoadState.LOADING);
    repository.getMetadata(
        new ExpenseRepository.Callback<MetadataDto>() {
          @Override
          public void onSuccess(MetadataDto value) {
            metadata.setValue(value);
            if (isEditing()) {
              loadExpense();
            } else {
              loadState.setValue(LoadState.READY);
            }
          }

          @Override
          public void onError(ApiError error) {
            loadFailed(error);
          }
        });
  }

  private void loadExpense() {
    repository.getExpense(
        expenseId,
        new ExpenseRepository.Callback<ExpenseDto>() {
          @Override
          public void onSuccess(ExpenseDto value) {
            selectedCategory.setValue(value.category);
            selectedDate.setValue(value.date);
            prefill.setValue(new Event<>(value));
            loadState.setValue(LoadState.READY);
          }

          @Override
          public void onError(ApiError error) {
            if (error.getKind() == ApiError.Kind.NOT_FOUND) {
              notice.setValue(new Event<>(error.getKind()));
              navigateBack.setValue(new Event<>(true));
            } else {
              loadFailed(error);
            }
          }
        });
  }

  private void loadFailed(ApiError error) {
    loadError.setValue(error);
    loadState.setValue(LoadState.ERROR);
  }

  private void submit(ExpenseRequestDto request) {
    saving.setValue(true);
    ExpenseRepository.Callback<ExpenseDto> callback =
        new ExpenseRepository.Callback<ExpenseDto>() {
          @Override
          public void onSuccess(ExpenseDto value) {
            saving.setValue(false);
            navigateBack.setValue(new Event<>(true));
          }

          @Override
          public void onError(ApiError error) {
            saving.setValue(false);
            saveFailed(error);
          }
        };
    if (isEditing()) {
      repository.updateExpense(expenseId, request, callback);
    } else {
      repository.createExpense(request, callback);
    }
  }

  private void saveFailed(ApiError error) {
    if (error.getKind() == ApiError.Kind.VALIDATION) {
      Map<Field, FormError> mapped = mapServerErrors(error);
      if (!mapped.isEmpty()) {
        fieldErrors.setValue(mapped);
        return;
      }
    }
    notice.setValue(new Event<>(error.getKind()));
    if (error.getKind() == ApiError.Kind.NOT_FOUND) {
      navigateBack.setValue(new Event<>(true));
    }
  }

  private static Map<Field, FormError> mapServerErrors(ApiError error) {
    Map<Field, FormError> mapped = new EnumMap<>(Field.class);
    for (FieldErrorDto fieldError : error.getFieldErrors()) {
      for (Field field : Field.values()) {
        if (field.name().equalsIgnoreCase(fieldError.field)) {
          mapped.put(field, new FormError(Reason.SERVER, fieldError.message));
        }
      }
    }
    return mapped;
  }

  private static BigDecimal parseAmount(String text, Map<Field, FormError> errors) {
    String trimmed = text == null ? "" : text.trim();
    if (trimmed.isEmpty()) {
      errors.put(Field.AMOUNT, new FormError(Reason.AMOUNT_REQUIRED, null));
      return null;
    }
    BigDecimal amount;
    try {
      amount = new BigDecimal(trimmed);
    } catch (NumberFormatException e) {
      errors.put(Field.AMOUNT, new FormError(Reason.AMOUNT_REQUIRED, null));
      return null;
    }
    if (amount.signum() <= 0) {
      errors.put(Field.AMOUNT, new FormError(Reason.AMOUNT_NOT_POSITIVE, null));
    } else if (amount.stripTrailingZeros().scale() > 2) {
      errors.put(Field.AMOUNT, new FormError(Reason.AMOUNT_TOO_MANY_DECIMALS, null));
    } else if (amount.compareTo(MAX_AMOUNT) > 0) {
      errors.put(Field.AMOUNT, new FormError(Reason.AMOUNT_TOO_LARGE, null));
    }
    return amount;
  }

  private static void validateCategory(String category, Map<Field, FormError> errors) {
    if (category == null || category.isEmpty()) {
      errors.put(Field.CATEGORY, new FormError(Reason.CATEGORY_REQUIRED, null));
    }
  }

  private void validateDate(LocalDate date, Map<Field, FormError> errors) {
    if (date == null) {
      errors.put(Field.DATE, new FormError(Reason.DATE_REQUIRED, null));
    } else if (date.isAfter(LocalDate.now(clock))) {
      errors.put(Field.DATE, new FormError(Reason.DATE_IN_FUTURE, null));
    }
  }
}
