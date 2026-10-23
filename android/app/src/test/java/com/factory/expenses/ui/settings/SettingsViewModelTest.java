package com.factory.expenses.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.factory.expenses.testing.FakeBaseUrlStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SettingsViewModelTest {

  @Rule public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

  private FakeBaseUrlStore store;
  private SettingsViewModel viewModel;

  @Before
  public void setUp() {
    store = new FakeBaseUrlStore("http://10.0.2.2:8080/");
    viewModel = new SettingsViewModel(store);
  }

  @Test
  public void exposesStoredUrl() {
    assertEquals("http://10.0.2.2:8080/", viewModel.getCurrentUrl().getValue());
  }

  @Test
  public void validUrlIsNormalisedPersistedAndAnnounced() {
    viewModel.save("http://192.168.1.10:8080");

    assertEquals("http://192.168.1.10:8080/", store.getBaseUrl());
    assertEquals("http://192.168.1.10:8080/", viewModel.getCurrentUrl().getValue());
    assertFalse(viewModel.getUrlInvalid().getValue());
    assertTrue(viewModel.getSaved().getValue().getContentIfNotHandled().isPresent());
  }

  @Test
  public void invalidUrlIsRejectedAndNotPersisted() {
    viewModel.save("ftp://x");

    assertTrue(viewModel.getUrlInvalid().getValue());
    assertEquals("http://10.0.2.2:8080/", store.getBaseUrl());
    assertNull(viewModel.getSaved().getValue());
  }

  @Test
  public void validSaveClearsEarlierError() {
    viewModel.save("not a url");
    viewModel.save("https://host/");

    assertFalse(viewModel.getUrlInvalid().getValue());
    assertEquals("https://host/", store.getBaseUrl());
  }
}
