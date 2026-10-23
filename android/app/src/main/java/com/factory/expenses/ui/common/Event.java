package com.factory.expenses.ui.common;

import java.util.Optional;

/** One-shot value delivered through LiveData, consumed by the first observer that reads it. */
public final class Event<T> {

  private final T content;
  private boolean handled;

  public Event(T content) {
    this.content = content;
  }

  /** Returns the content once; later calls return empty. */
  public Optional<T> getContentIfNotHandled() {
    if (handled) {
      return Optional.empty();
    }
    handled = true;
    return Optional.of(content);
  }

  public T peekContent() {
    return content;
  }
}
