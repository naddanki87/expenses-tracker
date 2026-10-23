package com.factory.expenses.ui.summary;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.factory.expenses.R;
import com.factory.expenses.data.api.dto.CategoryTotalDto;
import com.factory.expenses.data.api.dto.MetadataDto;
import com.factory.expenses.databinding.ItemCategoryTotalBinding;
import com.factory.expenses.ui.common.MoneyFormatter;
import java.util.Collections;
import java.util.List;

/** One row per category in the order the server returned them. */
public class CategoryTotalAdapter extends RecyclerView.Adapter<CategoryTotalAdapter.Holder> {

  private List<CategoryTotalDto> rows = Collections.emptyList();
  private MetadataDto metadata;

  public void submit(List<CategoryTotalDto> rows, MetadataDto metadata) {
    this.rows = rows;
    this.metadata = metadata;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemCategoryTotalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    CategoryTotalDto row = rows.get(position);
    ItemCategoryTotalBinding views = holder.views;
    views.totalCategory.setText(
        metadata == null ? row.category : metadata.displayName(row.category));
    views.totalAmount.setText(
        MoneyFormatter.format(row.total, metadata == null ? null : metadata.currency));
    views.totalCount.setText(
        views
            .getRoot()
            .getResources()
            .getQuantityString(R.plurals.expense_count, (int) row.count, (int) row.count));
  }

  @Override
  public int getItemCount() {
    return rows.size();
  }

  /** View holder for one category row. */
  public static final class Holder extends RecyclerView.ViewHolder {
    final ItemCategoryTotalBinding views;

    Holder(ItemCategoryTotalBinding views) {
      super(views.getRoot());
      this.views = views;
    }
  }
}
