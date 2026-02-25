package com.monetai.sample.kotlin.purchase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.revenuecat.purchases.EntitlementInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EntitlementAdapter extends RecyclerView.Adapter<EntitlementAdapter.EntitlementViewHolder> {

    private List<EntitlementInfo> entitlements = new ArrayList<>();

    public void updateEntitlements(List<EntitlementInfo> newEntitlements) {
        entitlements = newEntitlements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntitlementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entitlement, parent, false);
        return new EntitlementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntitlementViewHolder holder, int position) {
        holder.bind(entitlements.get(position));
    }

    @Override
    public int getItemCount() {
        return entitlements.size();
    }

    class EntitlementViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewEntitlementKey;
        private final TextView textViewEntitlementStatus;
        private final TextView textViewExpirationDate;

        EntitlementViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewEntitlementKey = itemView.findViewById(R.id.textViewEntitlementKey);
            textViewEntitlementStatus = itemView.findViewById(R.id.textViewEntitlementStatus);
            textViewExpirationDate = itemView.findViewById(R.id.textViewExpirationDate);
        }

        void bind(EntitlementInfo entitlement) {
            textViewEntitlementKey.setText(entitlement.getIdentifier());

            boolean isActive = entitlement.isActive();
            textViewEntitlementStatus.setText(isActive ? "ACTIVE" : "EXPIRED");
            textViewEntitlementStatus.setBackgroundResource(
                    isActive ? R.drawable.bg_success : R.drawable.bg_error
            );

            Date expirationDate = entitlement.getExpirationDate();
            if (expirationDate != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = formatter.format(expirationDate);
                textViewExpirationDate.setText("Expires: " + formattedDate);
            } else {
                textViewExpirationDate.setText("Lifetime access");
            }
            textViewExpirationDate.setVisibility(View.VISIBLE);
        }
    }
}
