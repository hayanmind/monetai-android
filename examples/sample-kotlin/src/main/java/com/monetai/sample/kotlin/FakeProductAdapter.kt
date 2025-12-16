package com.monetai.sample.kotlin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FakeProductAdapter(
    private val onVisible: (FakeProduct) -> Unit
) : RecyclerView.Adapter<FakeProductAdapter.ProductViewHolder>() {
    
    private var items: List<FakeProduct> = emptyList()
    
    fun submitList(newItems: List<FakeProduct>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fake_product, parent, false)
        return ProductViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        onVisible(item)
    }
    
    override fun getItemCount(): Int = items.size
    
    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textProductTitle)
        private val description: TextView = itemView.findViewById(R.id.textProductDescription)
        private val price: TextView = itemView.findViewById(R.id.textProductPrice)
        private val regularPrice: TextView = itemView.findViewById(R.id.textProductRegularPrice)
        
        fun bind(item: FakeProduct) {
            title.text = item.title
            description.text = item.description
            price.text = "${item.currencyCode} ${"%.2f".format(item.price)}"
            regularPrice.text = "${item.currencyCode} ${"%.2f".format(item.regularPrice)}"
        }
    }
}

