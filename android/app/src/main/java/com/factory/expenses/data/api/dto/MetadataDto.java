package com.factory.expenses.data.api.dto;

import java.util.List;

/** Currency and categories of the installation. */
public final class MetadataDto {
  public final String currency;
  public final List<CategoryInfoDto> categories;

  public MetadataDto(String currency, List<CategoryInfoDto> categories) {
    this.currency = currency;
    this.categories = categories;
  }

  /** Display name for a category code, falling back to the code itself. */
  public String displayName(String code) {
    for (CategoryInfoDto info : categories) {
      if (info.code.equals(code)) {
        return info.displayName;
      }
    }
    return code;
  }
}
