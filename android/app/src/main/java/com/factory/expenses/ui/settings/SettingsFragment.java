package com.factory.expenses.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.factory.expenses.R;
import com.factory.expenses.databinding.FragmentSettingsBinding;
import com.factory.expenses.ui.common.UiSupport;

/** Lets the user change the API base URL. */
public class SettingsFragment extends Fragment {

  private FragmentSettingsBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    SettingsViewModel viewModel = UiSupport.provider(this).get(SettingsViewModel.class);
    if (savedInstanceState == null) {
      binding.urlInput.setText(viewModel.getCurrentUrl().getValue());
    }
    binding.buttonSaveUrl.setOnClickListener(
        v -> viewModel.save(String.valueOf(binding.urlInput.getText())));
    viewModel
        .getUrlInvalid()
        .observe(
            getViewLifecycleOwner(),
            invalid ->
                binding.urlLayout.setError(
                    invalid ? getString(R.string.settings_url_invalid) : null));
    viewModel
        .getSaved()
        .observe(
            getViewLifecycleOwner(),
            event ->
                event
                    .getContentIfNotHandled()
                    .ifPresent(
                        done ->
                            Toast.makeText(
                                    requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT)
                                .show()));
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
