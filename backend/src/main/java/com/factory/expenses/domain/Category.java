package com.factory.expenses.domain;

public enum Category {
  FOOD("Food"),
  GROCERIES("Groceries"),
  TRANSPORT("Transport"),
  HOUSING("Housing"),
  UTILITIES("Utilities"),
  HEALTH("Health"),
  ENTERTAINMENT("Entertainment"),
  SHOPPING("Shopping"),
  EDUCATION("Education"),
  TRAVEL("Travel"),
  OTHER("Other");

  private final String displayName;

  Category(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
