package com.monetai.sample.kotlin

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.monetai.sample.kotlin.databinding.ActivityMainBinding
import com.monetai.sdk.MonetaiSDK
import com.monetai.sdk.models.PredictResult
import com.monetai.sdk.models.Feature
import com.monetai.sdk.models.PaywallConfig
import com.monetai.sdk.models.PaywallStyle
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // MARK: - UI Elements
    private lateinit var binding: ActivityMainBinding
    
    // MARK: - Paywall State
    private var isSubscriber: Boolean = false
    private var paywallConfig: PaywallConfig? = null

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
        
        // Initialize subscription UI
        updateSubscriptionUI()
    }

    // MARK: - UI Setup
    private fun setupUI() {
        // Set initial text
        binding.titleLabel.text = "MonetaiSDK Demo"
        binding.statusLabel.text = "SDK Status: Initializing..."
        binding.discountStatusLabel.text = "Discount: None"


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
                } else {
                    // SDK initialized successfully
                    updateSDKStatus()
                    
                    // Set up discount info change callback AFTER initialization
                    MonetaiSDK.shared.onDiscountInfoChange = { discountInfo ->
                        runOnUiThread {
                            println("🎯 Discount info changed callback triggered")
                            println("  discountInfo: $discountInfo")
                            println("  current time: ${Date()}")
                            if (discountInfo != null) {
                                println("  discount end time: ${discountInfo.endedAt}")
                                println("  is discount active: ${discountInfo.endedAt > Date()}")
                            }
                            handleDiscountInfoChange(discountInfo)
                        }
                    }
                    
                    // Set up paywall AFTER initialization
                    setupPaywall()
                }
            }
        }
    }

    private fun updateSDKStatus() {
        val isInitialized = MonetaiSDK.shared.getInitialized()
        binding.statusLabel.text = "SDK Status: ${if (isInitialized) "✅ Initialized" else "❌ Not Initialized"}"
        
        // Enable buttons only after initialization
        binding.predictButton.isEnabled = isInitialized
        binding.logEventButton.isEnabled = isInitialized
        binding.predictButton.alpha = if (isInitialized) 1.0f else 0.5f
        binding.logEventButton.alpha = if (isInitialized) 1.0f else 0.5f
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
                // Discount is valid - SDK will automatically show banner
                binding.discountStatusLabel.text = "Discount: ✅ Active (Expires: ${dateFormat.format(endTime)})"
                binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                println("🎯 Discount is active - SDK should automatically show banner")
            } else {
                // Discount expired
                binding.discountStatusLabel.text = "Discount: ❌ Expired"
                binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                println("❌ Discount expired - SDK should automatically hide banner")
            }
        } else {
            // No discount
            binding.discountStatusLabel.text = "Discount: None"
            binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            println("📭 No discount - SDK should automatically hide banner")
        }
    }



    // MARK: - Button Actions
    private fun predictButtonTapped() {
        MonetaiSDK.shared.predict { result, error ->
            runOnUiThread {
                if (error != null) {
                    println("Prediction failed: $error")
                } else {
                    result?.let { predictResult ->
                        println("Prediction result: ${predictResult.prediction}")
                        println("Test group: ${predictResult.testGroup}")

                        when (predictResult.prediction) {
                            PredictResult.NON_PURCHASER -> {
                                // When predicted as non-purchaser, SDK should automatically show paywall
                                println("Predicted as non-purchaser - SDK should automatically show paywall")
                                println("Note: If paywall doesn't appear, check SDK's automatic display logic")
                            }
                            PredictResult.PURCHASER -> {
                                // When predicted as purchaser
                                println("Predicted as purchaser - paywall not needed")
                            }
                            null -> {
                                // When prediction is null
                                println("No prediction available")
                            }
                        }

                        // Show alert with prediction result
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("AI Purchase Prediction Result")
                            .setMessage("Prediction: ${predictResult.prediction}\nTest Group: ${predictResult.testGroup}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }
    
    // MARK: - Paywall Methods
    private fun setupPaywall() {
        paywallConfig = PaywallConfig(
            discountPercent = 30,
            regularPrice = "$99.99",
            discountedPrice = "$69.99",
            locale = "ko",
            style = PaywallStyle.COMPACT,
            features = listOf(
                Feature(
                    title = "Unlimited Access",
                    description = "Use all premium features without limits",
                    isPremiumOnly = true
                ),
                Feature(
                    title = "Advanced Analytics",
                    description = "AI-powered insights",
                    isPremiumOnly = false
                ),
                Feature(
                    title = "Priority Support",
                    description = "24/7 customer support",
                    isPremiumOnly = false
                )
            ),
            enabled = true,
            bannerBottom = 20f,
            isSubscriber = isSubscriber,
            
            
            onPurchase = { paywallContext, closePaywall ->
                println("💰 MainActivity: onPurchase callback triggered")
                println("  📱 Activity: ${paywallContext.activity.javaClass.simpleName}")
                println("  🏗️ App Context: ${paywallContext.applicationContext.javaClass.simpleName}")
                
                // Process purchase immediately
                println("💰 Processing purchase...")
                // Update subscription status
                isSubscriber = true
                MonetaiSDK.shared.setSubscriptionStatus(true)
                updateSubscriptionUI()
                // Close paywall
                closePaywall()
                println("✅ Purchase completed successfully")
            },
            onTermsOfService = { paywallContext ->
                println("🔗 MainActivity: onTermsOfService callback triggered")
                println("  📱 Activity: ${paywallContext.activity.javaClass.simpleName}")
                showTermsOfService(paywallContext)
            },
            onPrivacyPolicy = { paywallContext ->
                println("🔗 MainActivity: onPrivacyPolicy callback triggered") 
                println("  📱 Activity: ${paywallContext.activity.javaClass.simpleName}")
                showPrivacyPolicy(paywallContext)
            }
        )
        
        paywallConfig?.let { config ->
            // Configure paywall
            MonetaiSDK.shared.configurePaywall(config)
            println("🎯 Paywall configured successfully")
            println("  config.enabled: ${config.enabled}")
            println("  config.isSubscriber: ${config.isSubscriber}")
            println("  config.bannerBottom: ${config.bannerBottom}")
        }
    }
    
    private fun updateSubscriptionUI() {
        val statusText = if (isSubscriber) "✅ Subscriber" else "❌ Non-subscriber"
        binding.subscriptionStatusLabel.text = "Subscription Status: $statusText"
        binding.subscriptionStatusLabel.setTextColor(
            if (isSubscriber) 
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else 
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )
    }
    
    
        private fun showTermsOfService(paywallContext: com.monetai.sdk.models.PaywallContext) {
        println("📄 MainActivity: showTermsOfService() called")
        println("  🎯 Context from: ${paywallContext.activity.javaClass.simpleName}")
        
        // Use the Activity from PaywallContext for better dialog management
        val contextActivity = paywallContext.activity
        
        // PaywallContext의 Activity는 이미 MonetaiPaywallActivity이므로 직접 사용
        contextActivity.runOnUiThread {
            val contextInfo = "Called from: ${paywallContext.activity.javaClass.simpleName}"
            
            // 가장 간단한 기본 다이얼로그 - PaywallActivity에서 직접 표시
            AlertDialog.Builder(contextActivity)
                .setTitle("Terms of Service")
                .setMessage("Terms of service content will be displayed here.\n\n$contextInfo")
                .setPositiveButton("OK", null)
                .show()
            
            println("✅ Terms of Service dialog shown on PaywallActivity: ${contextActivity.javaClass.simpleName}")
        }
    }
    
        private fun showPrivacyPolicy(paywallContext: com.monetai.sdk.models.PaywallContext) {
        println("🔒 MainActivity: showPrivacyPolicy() called")
        println("  🎯 Context from: ${paywallContext.activity.javaClass.simpleName}")
        
        // Use the Activity from PaywallContext for better dialog management
        val contextActivity = paywallContext.activity
        
        // PaywallContext의 Activity는 이미 MonetaiPaywallActivity이므로 직접 사용
        contextActivity.runOnUiThread {
            val contextInfo = "Called from: ${paywallContext.activity.javaClass.simpleName}"
            
            // 가장 간단한 기본 다이얼로그 - PaywallActivity에서 직접 표시
            AlertDialog.Builder(contextActivity)
                .setTitle("Privacy Policy")
                .setMessage("Privacy policy content will be displayed here.\n\n$contextInfo")
                .setPositiveButton("OK", null)
                .show()
            
            println("✅ Privacy Policy dialog shown on PaywallActivity: ${contextActivity.javaClass.simpleName}")
        }
    }

    private fun logEventButtonTapped() {
        // Log a sample event with parameters
        val params = mapOf(
            "button" to "test_button",
            "screen" to "main"
        )
        
        MonetaiSDK.shared.logEvent("button_click", params)



        println("Event logged: button_click")
    }
} 