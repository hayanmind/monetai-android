package com.monetai.sample.kotlin.purchase;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.monetai.sdk.models.Offer;
import com.monetai.sdk.models.OfferProduct;
import com.revenuecat.purchases.Package;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnPurchaseClickListener {
        void onPurchaseClick(Package packageItem);
    }

    private List<Package> products = new ArrayList<>();
    private Map<String, OfferProduct> offerProductMap = new HashMap<>();
    private Package basePackage = null;
    private final OnPurchaseClickListener onPurchaseClick;

    public ProductAdapter(OnPurchaseClickListener onPurchaseClick) {
        this.onPurchaseClick = onPurchaseClick;
    }

    public void updateProducts(List<Package> newProducts, @Nullable Offer offer, @Nullable Package basePackage) {
        products = newProducts;
        this.basePackage = basePackage;
        offerProductMap = new HashMap<>();
        if (offer != null) {
            for (OfferProduct product : offer.getProducts()) {
                offerProductMap.put(product.getSku(), product);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String toOfferSku(String identifier) {
        return identifier.replace(":base", "");
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewTitle;
        private final TextView textViewPrice;
        private final TextView textViewOriginalPrice;
        private final TextView textViewDiscountBadge;
        private final Button buttonPurchase;
        private final int defaultPriceColor;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewProductTitle);
            textViewPrice = itemView.findViewById(R.id.textViewProductPrice);
            textViewOriginalPrice = itemView.findViewById(R.id.textViewOriginalPrice);
            textViewDiscountBadge = itemView.findViewById(R.id.textViewDiscountBadge);
            buttonPurchase = itemView.findViewById(R.id.buttonBuy);
            defaultPriceColor = textViewPrice.getCurrentTextColor();
        }

        void bind(Package packageItem) {
            textViewTitle.setText(packageItem.getProduct().getTitle());
            textViewPrice.setText(packageItem.getProduct().getPrice().getFormatted());

            String sku = toOfferSku(packageItem.getProduct().getId());
            OfferProduct offerProduct = offerProductMap.get(sku);

            if (offerProduct != null && basePackage != null) {
                // Show strikethrough original price
                textViewOriginalPrice.setText(basePackage.getProduct().getPrice().getFormatted());
                textViewOriginalPrice.setPaintFlags(
                        textViewOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
                textViewOriginalPrice.setVisibility(View.VISIBLE);

                // Show discounted price in red
                textViewPrice.setTextColor(Color.RED);
                textViewPrice.setTypeface(null, Typeface.BOLD);

                // Show discount badge
                textViewDiscountBadge.setText("-" + (int) (offerProduct.getDiscountRate() * 100) + "%");
                textViewDiscountBadge.setVisibility(View.VISIBLE);
            } else {
                textViewOriginalPrice.setVisibility(View.GONE);
                textViewPrice.setTextColor(defaultPriceColor);
                textViewPrice.setTypeface(null, Typeface.NORMAL);
                textViewDiscountBadge.setVisibility(View.GONE);
            }

            buttonPurchase.setOnClickListener(v -> onPurchaseClick.onPurchaseClick(packageItem));
        }
    }
}
