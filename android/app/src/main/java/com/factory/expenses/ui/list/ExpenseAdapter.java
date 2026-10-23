package com.factory.expenses.ui.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.factory.expenses.data.api.dto.ExpenseDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.databinding.ItemExpenseBinding;
import com.factory.expenses.ui.common.MoneyFormatter;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Rows of the expenses list: category, date, note preview and formatted amount. */
public class ExpenseAdapter extends ListAdapter<ExpenseDto, ExpenseAdapter.Holder> {

  /** Receives taps on a row. */
  public interface OnExpenseClick {
    void onClick(ExpenseDto expense);
  }

  private static final DiffUtil.ItemCallback<ExpenseDto> DIFF =
      new DiffUtil.ItemCallback<ExpenseDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull ExpenseDto a, @NonNull ExpenseDto b) {
          return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ExpenseDto a, @NonNull ExpenseDto b) {
          return a.updatedAt.equals(b.updatedAt);
        }
      };

  private final OnExpenseClick listener;
  private final DateTimeFormatter dateFormat =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
  private MetadataDto metadata;

  public ExpenseAdapter(OnExpenseClick listener) {
    super(DIFF);
    this.listener = listener;
  }

  public void setMetadata(MetadataDto metadata) {
    this.metadata = metadata;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    ExpenseDto expense = getItem(position);
    ItemExpenseBinding views = holder.views;
    views.rowCategory.setText(
        metadata == null ? expense.category : metadata.displayName(expense.category));
    views.rowDate.setText(dateFormat.format(expense.date));
    views.rowAmount.setText(
        MoneyFormatter.format(expense.amount, metadata == null ? null : metadata.currency));
    views.rowNote.setText(expense.note);
    views.rowNote.setVisibility(expense.note == null ? View.GONE : View.VISIBLE);
    views.getRoot().setOnClickListener(v -> listener.onClick(expense));
  }

  /** View holder for one expense row. */
  public static final class Holder extends RecyclerView.ViewHolder {
    final ItemExpenseBinding views;

    Holder(ItemExpenseBinding views) {
      super(views.getRoot());
      this.views = views;
    }
  }
}
