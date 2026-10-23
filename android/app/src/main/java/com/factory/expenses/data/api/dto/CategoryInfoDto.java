package com.factory.expenses.data.api.dto;

/** Category code with its display name. */
public final class CategoryInfoDto {
  public final String code;
  public final String displayName;

  public CategoryInfoDto(String code, String displayName) {
    this.code = code;
    this.displayName = displayName;
  }
}
