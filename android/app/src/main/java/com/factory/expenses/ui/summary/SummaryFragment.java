package com.factory.expenses.ui.summary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.factory.expenses.R;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import com.factory.expenses.databinding.FragmentSummaryBinding;
import com.factory.expenses.ui.common.MoneyFormatter;
import com.factory.expenses.ui.common.UiSupport;

/** Total and per-category totals of one month. */
public class SummaryFragment extends Fragment {

  private FragmentSummaryBinding binding;
  private SummaryViewModel viewModel;
  private CategoryTotalAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    binding = FragmentSummaryBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = UiSupport.provider(this).get(SummaryViewModel.class);
    adapter = new CategoryTotalAdapter();
    binding.categoryList.setAdapter(adapter);
    binding.monthBar.buttonPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
    binding.monthBar.buttonNextMonth.setOnClickListener(v -> viewModel.nextMonth());
    binding.errorGroup.buttonRetry.setOnClickListener(v -> viewModel.retry());
    viewModel
        .getMonth()
        .observe(
            getViewLifecycleOwner(),
            month -> binding.monthBar.monthLabel.setText(UiSupport.monthLabel(month)));
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

  private void render(SummaryViewModel.State state) {
    SummaryViewModel.Status status = state.status;
    boolean hasSummary =
        status == SummaryViewModel.Status.CONTENT || status == SummaryViewModel.Status.EMPTY;
    binding.progress.setVisibility(visible(status == SummaryViewModel.Status.LOADING));
    binding.contentGroup.setVisibility(visible(hasSummary));
    binding.emptyText.setVisibility(visible(status == SummaryViewModel.Status.EMPTY));
    binding.errorGroup.getRoot().setVisibility(visible(status == SummaryViewModel.Status.ERROR));
    if (status == SummaryViewModel.Status.ERROR) {
      binding.errorGroup.errorText.setText(UiSupport.messageFor(state.error.getKind()));
    }
    if (hasSummary) {
      MonthlySummaryDto summary = state.summary;
      binding.totalValue.setText(MoneyFormatter.format(summary.total, summary.currency));
      binding.totalExpenseCount.setText(
          getResources()
              .getQuantityString(
                  R.plurals.expense_count, (int) summary.count, (int) summary.count));
      adapter.submit(summary.categories, viewModel.getMetadata().getValue());
    }
  }

  private static int visible(boolean show) {
    return show ? View.VISIBLE : View.GONE;
  }
}
