package com.factory.expenses.api.dto;

import com.factory.expenses.domain.Category;
import java.math.BigDecimal;

public record CategoryTotal(Category category, BigDecimal total, long count) {}
