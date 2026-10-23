package com.factory.expenses.ui.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.factory.expenses.R;
import com.factory.expenses.data.api.dto.CategoryInfoDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.databinding.FragmentListBinding;
import com.factory.expenses.ui.common.UiSupport;
import com.factory.expenses.ui.edit.EditFragment;
import java.util.ArrayList;
import java.util.List;

/** Expenses of one month, optionally filtered by category. */
public class ListFragment extends Fragment {

  private FragmentListBinding binding;
  private ListViewModel viewModel;
  private ExpenseAdapter adapter;
  private final List<String> categoryCodes = new ArrayList<>();

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    binding = FragmentListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = UiSupport.provider(this).get(ListViewModel.class);
    adapter = new ExpenseAdapter(expense -> openEditor(expense.id));
    binding.expenseList.setAdapter(adapter);
    binding.monthBar.buttonPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
    binding.monthBar.buttonNextMonth.setOnClickListener(v -> viewModel.nextMonth());
    binding.fabAdd.setOnClickListener(v -> openEditor(0L));
    binding.errorGroup.buttonRetry.setOnClickListener(v -> viewModel.retry());
    binding.categoryFilter.setOnItemClickListener(
        (parent, v, position, id) ->
            viewModel.setCategory(position == 0 ? null : categoryCodes.get(position - 1)));

    viewModel
        .getMonth()
        .observe(
            getViewLifecycleOwner(),
            month -> binding.monthBar.monthLabel.setText(UiSupport.monthLabel(month)));
    viewModel.getMetadata().observe(getViewLifecycleOwner(), this::showMetadata);
    viewModel.getCategory().observe(getViewLifecycleOwner(), code -> showSelectedCategory());
    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
  }

  @Override
  public void onStart() {
    super.onStart();
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private void openEditor(long expenseId) {
    Bundle args = new Bundle();
    args.putLong(EditFragment.ARG_EXPENSE_ID, expenseId);
    NavHostFragment.findNavController(this).navigate(R.id.action_list_to_edit, args);
  }

  private void showMetadata(MetadataDto metadata) {
    adapter.setMetadata(metadata);
    categoryCodes.clear();
    List<String> names = new ArrayList<>();
    names.add(getString(R.string.category_all));
    for (CategoryInfoDto info : metadata.categories) {
      categoryCodes.add(info.code);
      names.add(info.displayName);
    }
    binding.categoryFilter.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names));
    showSelectedCategory();
  }

  private void showSelectedCategory() {
    String code = viewModel.getCategory().getValue();
    MetadataDto metadata = viewModel.getMetadata().getValue();
    String label =
        code == null
            ? getString(R.string.category_all)
            : (metadata == null ? code : metadata.displayName(code));
    binding.categoryFilter.setText(label, false);
  }

  private void render(ListViewModel.State state) {
    boolean content = state.status == ListViewModel.Status.CONTENT;
    binding.progress.setVisibility(visible(state.status == ListViewModel.Status.LOADING));
    binding.expenseList.setVisibility(visible(content));
    binding.emptyText.setVisibility(visible(state.status == ListViewModel.Status.EMPTY));
    binding.errorGroup.getRoot().setVisibility(visible(state.status == ListViewModel.Status.ERROR));
    if (state.status == ListViewModel.Status.ERROR) {
      binding.errorGroup.errorText.setText(UiSupport.messageFor(state.error.getKind()));
    }
    adapter.submitList(state.expenses);
  }

  private static int visible(boolean show) {
    return show ? View.VISIBLE : View.GONE;
  }
}
