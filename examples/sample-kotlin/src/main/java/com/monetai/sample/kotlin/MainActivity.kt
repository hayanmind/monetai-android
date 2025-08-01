package com.monetai.sample.kotlin

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.monetai.sample.kotlin.databinding.ActivityMainBinding
import com.monetai.sdk.MonetaiSDK
import com.monetai.sample.kotlin.views.DiscountBannerView
import com.monetai.sdk.models.PredictResult
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // MARK: - UI Elements
    private lateinit var binding: ActivityMainBinding
    private var discountBannerView: DiscountBannerView? = null

    // MARK: - Constants
    companion object {
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        
        // Log app launch event BEFORE SDK initialization
        MonetaiSDK.shared.logEvent("app_in")
        
        setupMonetaiSDK()
    }

    // MARK: - UI Setup
    private fun setupUI() {
        // Set initial text
        binding.titleLabel.text = "MonetaiSDK Demo"
        binding.statusLabel.text = "SDK Status: Initializing..."
        binding.discountStatusLabel.text = "Discount: None"
        binding.resultLabel.text = "Tap buttons to test SDK functionality"

        // Setup button click listeners
        binding.predictButton.setOnClickListener { predictButtonTapped() }
        binding.logEventButton.setOnClickListener { logEventButtonTapped() }

        // Disable buttons initially
        binding.predictButton.isEnabled = false
        binding.logEventButton.isEnabled = false
        binding.predictButton.alpha = 0.5f
        binding.logEventButton.alpha = 0.5f
    }

    // MARK: - MonetaiSDK Setup
    private fun setupMonetaiSDK() {
        // Initialize SDK FIRST
        MonetaiSDK.shared.initialize(
            context = this@MainActivity,
            sdkKey = Constants.SDK_KEY,
            userId = Constants.USER_ID
        ) { result, error ->
            runOnUiThread {
                if (error != null) {
                    // SDK initialization failed
                    updateSDKStatus()
                    binding.resultLabel.text = "❌ SDK initialization failed: ${error.message}"
                    binding.resultLabel.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                } else {
                    // SDK initialized successfully
                    updateSDKStatus()
                    binding.resultLabel.text = "✅ SDK initialized successfully!"
                    binding.resultLabel.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                    
                    // Set up discount info change callback AFTER initialization
                    MonetaiSDK.shared.onDiscountInfoChange = { discountInfo ->
                        runOnUiThread {
                            handleDiscountInfoChange(discountInfo)
                        }
                    }
                }
            }
        }
    }

    private fun updateSDKStatus() {
        val isInitialized = MonetaiSDK.shared.getInitialized()
        
        if (isInitialized) {
            binding.statusLabel.text = "SDK Status: ✅ Ready"
            binding.statusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            
            // Enable buttons
            binding.predictButton.isEnabled = true
            binding.logEventButton.isEnabled = true
            binding.predictButton.alpha = 1.0f
            binding.logEventButton.alpha = 1.0f
        } else {
            binding.statusLabel.text = "SDK Status: ⏳ Initializing..."
            binding.statusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            
            // Disable buttons
            binding.predictButton.isEnabled = false
            binding.logEventButton.isEnabled = false
            binding.predictButton.alpha = 0.5f
            binding.logEventButton.alpha = 0.5f
        }
    }

    private fun handleDiscountInfoChange(discountInfo: com.monetai.sdk.models.AppUserDiscount?) {
        if (discountInfo != null) {
            val now = Date()
            val endTime = discountInfo.endedAt

            // Debug time comparison
            println("🔍 Time Debug:")
            println("  Current time (local): $now")
            println("  End time (from API): $endTime")
            println("  Time comparison: now < endTime = ${now < endTime}")
            println("  Time difference (ms): ${endTime.time - now.time}")

            if (now < endTime) {
                // Discount is valid - show banner
                binding.discountStatusLabel.text = "Discount: ✅ Active (Expires: ${dateFormat.format(endTime)})"
                binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                showDiscountBanner(discountInfo)
            } else {
                // Discount expired
                binding.discountStatusLabel.text = "Discount: ❌ Expired"
                binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                hideDiscountBanner()
            }
        } else {
            // No discount
            binding.discountStatusLabel.text = "Discount: None"
            binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            hideDiscountBanner()
        }
    }

    private fun showDiscountBanner(discount: com.monetai.sdk.models.AppUserDiscount) {
        // Remove existing banner if any
        hideDiscountBanner()

        // Create and add new banner
        discountBannerView = DiscountBannerView(this)
        
        // Debug logging
        println("🎯 showDiscountBanner called")
        println("  discountBannerView created: ${discountBannerView != null}")
        println("  rootLayout: ${binding.rootLayout}")
        
        // Add banner to root layout
        binding.rootLayout.addView(discountBannerView)
        
        // Set layout parameters to position at bottom
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = 20
            leftMargin = 16
            rightMargin = 16
        }
        discountBannerView?.layoutParams = layoutParams
        
        println("  Banner added to rootLayout")
        println("  Banner visibility before showDiscount: ${discountBannerView?.visibility}")

        // Show discount
        discountBannerView?.showDiscount(discount)

        // Update result label
        binding.resultLabel.text = "🎉 Discount banner displayed!\nSpecial offer is now active."
        binding.resultLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        
        println("  showDiscountBanner completed")
    }

    private fun hideDiscountBanner() {
        discountBannerView?.hideDiscount()
        discountBannerView = null
    }

    // MARK: - Button Actions
    private fun predictButtonTapped() {
        MonetaiSDK.shared.predict { result, error ->
            runOnUiThread {
                if (error != null) {
                    binding.resultLabel.text = "❌ Prediction failed: ${error.message}"
                    binding.resultLabel.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                    println("Prediction failed: $error")
                } else {
                    result?.let { predictResult ->
                        println("Prediction result: ${predictResult.prediction}")
                        println("Test group: ${predictResult.testGroup}")

                        when (predictResult?.prediction) {
                            PredictResult.NON_PURCHASER -> {
                                // When predicted as non-purchaser, offer discount
                                println("Predicted as non-purchaser - discount can be applied")
                            }
                            PredictResult.PURCHASER -> {
                                // When predicted as purchaser
                                println("Predicted as purchaser - discount not needed")
                            }
                            null -> {
                                // When prediction is null
                                println("No prediction available")
                            }
                        }

                        binding.resultLabel.text = "✅ Prediction completed - check console for details"
                        binding.resultLabel.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))

                        // Show alert with prediction result
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Purchase Prediction")
                            .setMessage("Prediction: ${predictResult.prediction}\nTest Group: ${predictResult.testGroup}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun logEventButtonTapped() {
        // Log a sample event with parameters
        val params = mapOf(
            "button" to "test_button",
            "screen" to "main"
        )
        
        MonetaiSDK.shared.logEvent("button_click", params)

        binding.resultLabel.text = "✅ Event logged: button_click\nParameters: button=test_button, screen=main"
        binding.resultLabel.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))

        println("Event logged: button_click")
    }
} 