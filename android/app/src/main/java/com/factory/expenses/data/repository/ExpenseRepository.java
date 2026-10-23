package com.factory.expenses.data.repository;

import com.factory.expenses.data.api.ExpenseApi;
import com.factory.expenses.data.api.JsonSupport;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.ExpenseRequestDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import com.factory.expenses.data.api.dto.ProblemDto;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Single entry point to the backend. Every call reports through a {@link Callback}; no exception
 * escapes. The Retrofit client is rebuilt whenever the base URL in the {@link BaseUrlStore}
 * changes.
 */
public class ExpenseRepository {

  /** Receives the outcome of one call. */
  public interface Callback<T> {
    void onSuccess(T value);

    void onError(ApiError error);
  }

  private interface CallFactory<T> {
    Call<T> create();
  }

  private final BaseUrlStore baseUrlStore;
  private final OkHttpClient httpClient;
  private final Gson gson = JsonSupport.gson();

  private ExpenseApi api;
  private String apiBaseUrl;
  private MetadataDto cachedMetadata;

  public ExpenseRepository(BaseUrlStore baseUrlStore, OkHttpClient httpClient) {
    this.baseUrlStore = baseUrlStore;
    this.httpClient = httpClient;
  }

  public void listExpenses(YearMonth month, String category, Callback<List<ExpenseDto>> callback) {
    run(callback, () -> api().listExpenses(month, category));
  }

  public void getExpense(long id, Callback<ExpenseDto> callback) {
    run(callback, () -> api().getExpense(id));
  }

  public void createExpense(ExpenseRequestDto request, Callback<ExpenseDto> callback) {
    run(callback, () -> api().createExpense(request));
  }

  public void updateExpense(long id, ExpenseRequestDto request, Callback<ExpenseDto> callback) {
    run(callback, () -> api().updateExpense(id, request));
  }

  public void deleteExpense(long id, Callback<Void> callback) {
    run(callback, () -> api().deleteExpense(id));
  }

  public void getMonthlySummary(YearMonth month, Callback<MonthlySummaryDto> callback) {
    run(callback, () -> api().getMonthlySummary(month));
  }

  /** Returns the cached metadata when available, otherwise fetches and caches it. */
  public void getMetadata(Callback<MetadataDto> callback) {
    MetadataDto cached = readCachedMetadata();
    if (cached != null) {
      callback.onSuccess(cached);
      return;
    }
    run(
        new Callback<MetadataDto>() {
          @Override
          public void onSuccess(MetadataDto value) {
            storeMetadata(value);
            callback.onSuccess(value);
          }

          @Override
          public void onError(ApiError error) {
            callback.onError(error);
          }
        },
        () -> api().getMetadata());
  }

  private synchronized MetadataDto readCachedMetadata() {
    return baseUrlStore.getBaseUrl().equals(apiBaseUrl) ? cachedMetadata : null;
  }

  private synchronized void storeMetadata(MetadataDto metadata) {
    cachedMetadata = metadata;
  }

  private synchronized ExpenseApi api() {
    String baseUrl = baseUrlStore.getBaseUrl();
    if (api == null || !baseUrl.equals(apiBaseUrl)) {
      api =
          new Retrofit.Builder()
              .baseUrl(baseUrl)
              .client(httpClient)
              .addConverterFactory(GsonConverterFactory.create(gson))
              .build()
              .create(ExpenseApi.class);
      apiBaseUrl = baseUrl;
      cachedMetadata = null;
    }
    return api;
  }

  private <T> void run(Callback<T> callback, CallFactory<T> factory) {
    Call<T> call;
    try {
      call = factory.create();
    } catch (IllegalArgumentException e) {
      callback.onError(ApiError.network(e.getMessage()));
      return;
    }
    call.enqueue(
        new retrofit2.Callback<T>() {
          @Override
          public void onResponse(Call<T> call, Response<T> response) {
            if (response.isSuccessful()) {
              callback.onSuccess(response.body());
            } else {
              callback.onError(ApiError.fromHttp(response.code(), parseProblem(response)));
            }
          }

          @Override
          public void onFailure(Call<T> call, Throwable t) {
            callback.onError(ApiError.network(t.getMessage()));
          }
        });
  }

  private ProblemDto parseProblem(Response<?> response) {
    try (ResponseBody body = response.errorBody()) {
      if (body == null) {
        return null;
      }
      return gson.fromJson(body.string(), ProblemDto.class);
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }
}
