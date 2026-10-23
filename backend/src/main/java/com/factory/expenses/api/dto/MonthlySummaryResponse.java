package com.factory.expenses.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
    String month, String currency, BigDecimal total, long count, List<CategoryTotal> categories) {}
