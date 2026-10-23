package com.factory.expenses.data.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/** Gson configuration with java.time adapters. BigDecimal keeps its scale by default. */
public final class JsonSupport {

  private JsonSupport() {}

  public static Gson gson() {
    return new GsonBuilder()
        .registerTypeAdapter(
            LocalDate.class,
            (JsonSerializer<LocalDate>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(
            LocalDate.class,
            (JsonDeserializer<LocalDate>) (json, type, ctx) -> LocalDate.parse(json.getAsString()))
        .registerTypeAdapter(
            YearMonth.class,
            (JsonSerializer<YearMonth>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(
            YearMonth.class,
            (JsonDeserializer<YearMonth>) (json, type, ctx) -> YearMonth.parse(json.getAsString()))
        .registerTypeAdapter(
            Instant.class,
            (JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(
            Instant.class,
            (JsonDeserializer<Instant>) (json, type, ctx) -> Instant.parse(json.getAsString()))
        .create();
  }
}
