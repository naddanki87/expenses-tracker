package com.factory.expenses.repository;

import com.factory.expenses.domain.Category;
import java.math.BigDecimal;

public record CategoryAggregate(Category category, BigDecimal total, long count) {}
