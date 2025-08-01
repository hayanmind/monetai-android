package com.monetai.sample.kotlin.purchase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.EntitlementInfo
import java.text.SimpleDateFormat
import java.util.*

class EntitlementAdapter : RecyclerView.Adapter<EntitlementAdapter.EntitlementViewHolder>() {
    
    private var entitlements: List<EntitlementInfo> = emptyList()
    
    fun updateEntitlements(newEntitlements: List<EntitlementInfo>) {
        entitlements = newEntitlements
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntitlementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entitlement, parent, false)
        return EntitlementViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: EntitlementViewHolder, position: Int) {
        holder.bind(entitlements[position])
    }
    
    override fun getItemCount(): Int = entitlements.size
    
    inner class EntitlementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewEntitlementKey: TextView = itemView.findViewById(R.id.textViewEntitlementKey)
        private val textViewEntitlementStatus: TextView = itemView.findViewById(R.id.textViewEntitlementStatus)
        private val textViewExpirationDate: TextView = itemView.findViewById(R.id.textViewExpirationDate)
        
        fun bind(entitlement: EntitlementInfo) {
            // Entitlement 키 표시
            textViewEntitlementKey.text = entitlement.identifier
            
            // 상태 표시 (Active/Expired)
            val isActive = entitlement.isActive
            textViewEntitlementStatus.text = if (isActive) "ACTIVE" else "EXPIRED"
            textViewEntitlementStatus.setBackgroundResource(
                if (isActive) R.drawable.bg_success else R.drawable.bg_error
            )
            
            // 만료 날짜 표시
            entitlement.expirationDate?.let { expirationDate ->
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val formattedDate = formatter.format(expirationDate)
                textViewExpirationDate.text = "Expires: $formattedDate"
                textViewExpirationDate.visibility = View.VISIBLE
            } ?: run {
                textViewExpirationDate.text = "Lifetime access"
                textViewExpirationDate.visibility = View.VISIBLE
            }
        }
    }
} 