package com.factory.expenses.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.factory.expenses.R;
import com.factory.expenses.databinding.ActivityMainBinding;

/** Single activity hosting the navigation graph and the bottom navigation bar. */
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    Fragment host = getSupportFragmentManager().findFragmentById(R.id.nav_host);
    NavController navController = ((NavHostFragment) host).getNavController();
    NavigationUI.setupWithNavController(binding.bottomNav, navController);
  }
}
