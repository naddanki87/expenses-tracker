package com.factory.expenses.ui.edit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.factory.expenses.R;
import com.factory.expenses.data.api.dto.CategoryInfoDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.databinding.FragmentEditBinding;
import com.factory.expenses.ui.common.UiSupport;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Add or edit one expense; editing also offers delete. */
public class EditFragment extends Fragment {

  public static final String ARG_EXPENSE_ID = "expenseId";

  private final DateTimeFormatter dateFormat =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
  private final List<String> categoryCodes = new ArrayList<>();
  private FragmentEditBinding binding;
  private EditViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    binding = FragmentEditBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = UiSupport.provider(this).get(EditViewModel.class);
    long expenseId = requireArguments().getLong(ARG_EXPENSE_ID, EditViewModel.NEW_EXPENSE);
    viewModel.start(expenseId);
    binding.editTitle.setText(viewModel.isEditing() ? R.string.title_edit : R.string.title_add);
    binding.buttonDelete.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);

    binding.buttonSave.setOnClickListener(
        v ->
            viewModel.save(
                String.valueOf(binding.amountInput.getText()),
                String.valueOf(binding.noteInput.getText())));
    binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    binding.dateInput.setOnClickListener(v -> pickDate());
    binding.errorGroup.buttonRetry.setOnClickListener(v -> viewModel.retry());
    binding.categoryInput.setOnItemClickListener(
        (parent, v, position, id) -> viewModel.setCategory(categoryCodes.get(position)));

    observeState();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private void observeState() {
    viewModel.getLoadState().observe(getViewLifecycleOwner(), this::renderLoadState);
    viewModel.getMetadata().observe(getViewLifecycleOwner(), this::showCategories);
    viewModel.getSelectedCategory().observe(getViewLifecycleOwner(), code -> showCategory());
    viewModel
        .getSelectedDate()
        .observe(
            getViewLifecycleOwner(),
            date -> binding.dateInput.setText(date == null ? "" : dateFormat.format(date)));
    viewModel.getFieldErrors().observe(getViewLifecycleOwner(), this::showFieldErrors);
    viewModel
        .getSaving()
        .observe(
            getViewLifecycleOwner(),
            saving -> {
              binding.buttonSave.setEnabled(!saving);
              binding.buttonDelete.setEnabled(!saving);
            });
    viewModel
        .getPrefill()
        .observe(
            getViewLifecycleOwner(),
            event ->
                event
                    .getContentIfNotHandled()
                    .ifPresent(
                        expense -> {
                          binding.amountInput.setText(expense.amount.toPlainString());
                          binding.noteInput.setText(expense.note);
                        }));
    viewModel
        .getNotice()
        .observe(
            getViewLifecycleOwner(),
            event ->
                event
                    .getContentIfNotHandled()
                    .ifPresent(
                        kind ->
                            Toast.makeText(
                                    requireContext(), UiSupport.messageFor(kind), Toast.LENGTH_LONG)
                                .show()));
    viewModel
        .getNavigateBack()
        .observe(
            getViewLifecycleOwner(),
            event ->
                event
                    .getContentIfNotHandled()
                    .ifPresent(done -> NavHostFragment.findNavController(this).popBackStack()));
  }

  private void renderLoadState(EditViewModel.LoadState state) {
    binding.progress.setVisibility(visible(state == EditViewModel.LoadState.LOADING));
    binding.formGroup.setVisibility(visible(state == EditViewModel.LoadState.READY));
    binding.errorGroup.getRoot().setVisibility(visible(state == EditViewModel.LoadState.ERROR));
    if (state == EditViewModel.LoadState.ERROR && viewModel.getLoadError().getValue() != null) {
      binding.errorGroup.errorText.setText(
          UiSupport.messageFor(viewModel.getLoadError().getValue().getKind()));
    }
  }

  private void showCategories(MetadataDto metadata) {
    categoryCodes.clear();
    List<String> names = new ArrayList<>();
    for (CategoryInfoDto info : metadata.categories) {
      categoryCodes.add(info.code);
      names.add(info.displayName);
    }
    binding.categoryInput.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names));
    showCategory();
  }

  private void showCategory() {
    String code = viewModel.getSelectedCategory().getValue();
    MetadataDto metadata = viewModel.getMetadata().getValue();
    if (code == null) {
      binding.categoryInput.setText("", false);
    } else {
      binding.categoryInput.setText(metadata == null ? code : metadata.displayName(code), false);
    }
  }

  private void showFieldErrors(Map<EditViewModel.Field, EditViewModel.FormError> errors) {
    binding.amountLayout.setError(errorText(errors.get(EditViewModel.Field.AMOUNT)));
    binding.categoryLayout.setError(errorText(errors.get(EditViewModel.Field.CATEGORY)));
    binding.dateLayout.setError(errorText(errors.get(EditViewModel.Field.DATE)));
    binding.noteLayout.setError(errorText(errors.get(EditViewModel.Field.NOTE)));
  }

  @Nullable
  private String errorText(@Nullable EditViewModel.FormError error) {
    if (error == null) {
      return null;
    }
    if (error.reason == EditViewModel.Reason.SERVER) {
      return error.message;
    }
    return getString(messageFor(error.reason));
  }

  @StringRes
  private static int messageFor(EditViewModel.Reason reason) {
    switch (reason) {
      case AMOUNT_REQUIRED:
        return R.string.error_amount_required;
      case AMOUNT_NOT_POSITIVE:
        return R.string.error_amount_not_positive;
      case AMOUNT_TOO_MANY_DECIMALS:
        return R.string.error_amount_decimals;
      case AMOUNT_TOO_LARGE:
        return R.string.error_amount_too_large;
      case CATEGORY_REQUIRED:
        return R.string.error_category_required;
      case DATE_REQUIRED:
        return R.string.error_date_required;
      case DATE_IN_FUTURE:
        return R.string.error_date_future;
      case NOTE_TOO_LONG:
      default:
        return R.string.error_note_too_long;
    }
  }

  private void pickDate() {
    long todayMillis = toUtcMillis(viewModel.today());
    LocalDate selected = viewModel.getSelectedDate().getValue();
    CalendarConstraints constraints =
        new CalendarConstraints.Builder()
            .setEnd(todayMillis)
            .setValidator(DateValidatorPointBackward.before(todayMillis))
            .build();
    MaterialDatePicker<Long> picker =
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setCalendarConstraints(constraints)
            .setSelection(toUtcMillis(selected == null ? viewModel.today() : selected))
            .build();
    picker.addOnPositiveButtonClickListener(
        millis ->
            viewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()));
    picker.show(getChildFragmentManager(), "date");
  }

  private void confirmDelete() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete_title)
        .setMessage(R.string.delete_message)
        .setNegativeButton(R.string.action_cancel, null)
        .setPositiveButton(R.string.action_delete, (dialog, which) -> viewModel.delete())
        .show();
  }

  private static long toUtcMillis(LocalDate date) {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
  }

  private static int visible(boolean show) {
    return show ? View.VISIBLE : View.GONE;
  }
}
