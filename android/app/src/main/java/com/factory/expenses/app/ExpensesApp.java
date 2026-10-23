package com.factory.expenses.app;

import android.app.Application;

/** Application class owning the dependency container. */
public class ExpensesApp extends Application {

  private AppContainer container;

  @Override
  public void onCreate() {
    super.onCreate();
    container = new AppContainer(this);
  }

  public AppContainer container() {
    return container;
  }
}
