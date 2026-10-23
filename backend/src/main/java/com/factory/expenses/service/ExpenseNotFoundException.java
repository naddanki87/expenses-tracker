package com.factory.expenses.service;

public class ExpenseNotFoundException extends RuntimeException {

  private final long expenseId;

  public ExpenseNotFoundException(long expenseId) {
    super("Expense " + expenseId + " not found");
    this.expenseId = expenseId;
  }

  public long getExpenseId() {
    return expenseId;
  }
}
