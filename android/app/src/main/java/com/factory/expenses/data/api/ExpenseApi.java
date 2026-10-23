package com.factory.expenses.data.api;

import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.ExpenseRequestDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.data.api.dto.MonthlySummaryDto;
import java.time.YearMonth;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Retrofit mirror of docs/openapi.yaml. */
public interface ExpenseApi {

  @GET("api/v1/expenses")
  Call<List<ExpenseDto>> listExpenses(
      @Query("month") YearMonth month, @Query("category") String category);

  @POST("api/v1/expenses")
  Call<ExpenseDto> createExpense(@Body ExpenseRequestDto body);

  @GET("api/v1/expenses/{id}")
  Call<ExpenseDto> getExpense(@Path("id") long id);

  @PUT("api/v1/expenses/{id}")
  Call<ExpenseDto> updateExpense(@Path("id") long id, @Body ExpenseRequestDto body);

  @DELETE("api/v1/expenses/{id}")
  Call<Void> deleteExpense(@Path("id") long id);

  @GET("api/v1/summaries/monthly")
  Call<MonthlySummaryDto> getMonthlySummary(@Query("month") YearMonth month);

  @GET("api/v1/metadata")
  Call<MetadataDto> getMetadata();
}
