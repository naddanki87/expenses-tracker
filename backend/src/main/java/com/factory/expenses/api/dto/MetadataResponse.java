package com.factory.expenses.api.dto;

import java.util.List;

public record MetadataResponse(String currency, List<CategoryInfo> categories) {}
