package com.monetai.sample.kotlin.purchase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Package

class ProductAdapter(
    private val onPurchaseClick: (Package) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    
    private var products: List<Package> = emptyList()
    
    fun updateProducts(newProducts: List<Package>) {
        products = newProducts
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }
    
    override fun getItemCount(): Int = products.size
    
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewProductTitle)
        private val textViewPrice: TextView = itemView.findViewById(R.id.textViewProductPrice)
        private val buttonPurchase: Button = itemView.findViewById(R.id.buttonBuy)
        
        fun bind(packageItem: Package) {
            // Extract product information from RevenueCat Package object
            textViewTitle.text = packageItem.product.title
            textViewPrice.text = packageItem.product.price.formatted
            
            buttonPurchase.setOnClickListener {
                onPurchaseClick(packageItem)
            }
        }
    }
} 