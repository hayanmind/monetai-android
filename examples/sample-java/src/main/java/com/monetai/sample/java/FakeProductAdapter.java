package com.monetai.sample.java;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FakeProductAdapter extends RecyclerView.Adapter<FakeProductAdapter.ProductViewHolder> {

    public interface OnVisibleListener {
        void onVisible(FakeProduct product);
    }

    private final OnVisibleListener onVisibleListener;
    private List<FakeProduct> items = new ArrayList<>();

    public FakeProductAdapter(OnVisibleListener onVisibleListener) {
        this.onVisibleListener = onVisibleListener;
    }

    public void submitList(List<FakeProduct> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fake_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        FakeProduct item = items.get(position);
        holder.bind(item);
        if (onVisibleListener != null) {
            onVisibleListener.onVisible(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;
        private final TextView price;
        private final TextView regularPrice;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textProductTitle);
            description = itemView.findViewById(R.id.textProductDescription);
            price = itemView.findViewById(R.id.textProductPrice);
            regularPrice = itemView.findViewById(R.id.textProductRegularPrice);
        }

        void bind(FakeProduct item) {
            title.setText(item.title);
            description.setText(item.description);
            price.setText(item.currencyCode + " " + String.format("%.2f", item.price));
            regularPrice.setText(item.currencyCode + " " + String.format("%.2f", item.regularPrice));
        }
    }
}

