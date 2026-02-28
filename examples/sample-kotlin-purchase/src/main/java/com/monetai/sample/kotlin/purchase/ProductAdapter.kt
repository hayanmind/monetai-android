package com.monetai.sample.kotlin.purchase

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.monetai.sdk.models.Offer
import com.monetai.sdk.models.OfferProduct
import com.revenuecat.purchases.Package

class ProductAdapter(
    private val onPurchaseClick: (Package) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var products: List<Package> = emptyList()
    private var offerProductMap: Map<String, OfferProduct> = emptyMap()
    private var basePackage: Package? = null

    fun updateProducts(
        newProducts: List<Package>,
        offer: Offer? = null,
        basePackage: Package? = null
    ) {
        products = newProducts
        this.basePackage = basePackage
        offerProductMap = offer?.products?.associateBy { it.sku } ?: emptyMap()
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

    private fun toOfferSku(identifier: String): String {
        return identifier.replace(":base", "")
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewProductTitle)
        private val textViewPrice: TextView = itemView.findViewById(R.id.textViewProductPrice)
        private val textViewOriginalPrice: TextView = itemView.findViewById(R.id.textViewOriginalPrice)
        private val textViewDiscountBadge: TextView = itemView.findViewById(R.id.textViewDiscountBadge)
        private val buttonPurchase: Button = itemView.findViewById(R.id.buttonBuy)
        private val defaultPriceColor = textViewPrice.currentTextColor

        fun bind(packageItem: Package) {
            textViewTitle.text = packageItem.product.title
            textViewPrice.text = packageItem.product.price.formatted

            val sku = toOfferSku(packageItem.product.id)
            val offerProduct = offerProductMap[sku]

            if (offerProduct != null && basePackage != null) {
                // Show strikethrough original price
                textViewOriginalPrice.text = basePackage!!.product.price.formatted
                textViewOriginalPrice.paintFlags =
                    textViewOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                textViewOriginalPrice.visibility = View.VISIBLE

                // Show discounted price in red
                textViewPrice.setTextColor(Color.RED)
                textViewPrice.setTypeface(null, android.graphics.Typeface.BOLD)

                // Show discount badge
                textViewDiscountBadge.text = "-${(offerProduct.discountRate * 100).toInt()}%"
                textViewDiscountBadge.visibility = View.VISIBLE
            } else {
                textViewOriginalPrice.visibility = View.GONE
                textViewPrice.setTextColor(defaultPriceColor)
                textViewPrice.setTypeface(null, android.graphics.Typeface.NORMAL)
                textViewDiscountBadge.visibility = View.GONE
            }

            buttonPurchase.setOnClickListener {
                onPurchaseClick(packageItem)
            }
        }
    }
}
