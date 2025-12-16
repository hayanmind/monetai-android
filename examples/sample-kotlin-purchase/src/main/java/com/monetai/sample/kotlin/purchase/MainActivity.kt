package com.monetai.sample.kotlin.purchase

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.monetai.sdk.MonetaiSDK
import com.monetai.sdk.models.LogEventOptions
import com.monetai.sdk.models.ViewProductItemParams
import com.monetai.sample.kotlin.purchase.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var monetaiSDK: MonetaiSDK
    private lateinit var productAdapter: ProductAdapter
    private lateinit var entitlementAdapter: EntitlementAdapter
    
    private var isInitialized = false
    private var predictionResult: String = ""
    private var isLoading = false
    
    private val sdkKey = Constants.SDK_KEY
    private val userId = Constants.USER_ID
    private val revenueCatAPIKey = Constants.REVENUE_CAT_API_KEY
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        updateSDKStatusDetails() // Initialize status display
        initializeSDKs()
    }
    
    private fun setupUI() {
        // Set up RecyclerView for products
        productAdapter = ProductAdapter(
            onPurchaseClick = { packageItem ->
                purchasePackage(packageItem)
            },
            onProductVisible = { packageItem ->
                logProductViewed(packageItem)
            }
        )
        
        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = productAdapter
        }
        
        // Set up RecyclerView for entitlements
        entitlementAdapter = EntitlementAdapter()
        
        binding.recyclerViewEntitlements.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = entitlementAdapter
        }
        
        // Set up click listeners
        binding.buttonPredictUser.setOnClickListener {
            predictUser()
        }
        
        binding.buttonAppOpened.setOnClickListener {
            logTestEvent("app_opened")
        }
        
        binding.buttonProductViewed.setOnClickListener {
            logTestEvent("product_viewed")
        }
        
        binding.buttonAddToCart.setOnClickListener {
            logTestEvent("add_to_cart", mapOf("value" to 9.99))
        }
        
        binding.buttonPurchaseStarted.setOnClickListener {
            logTestEvent("purchase_started", mapOf("value" to 19.99))
        }
        
        binding.buttonScreenView.setOnClickListener {
            logTestEvent("screen_view", mapOf(
                "screen_name" to "home",
                "previous_screen" to "onboarding"
            ))
        }
        
        binding.buttonButtonClick.setOnClickListener {
            logTestEvent("button_click", mapOf(
                "button_name" to "upgrade",
                "button_location" to "home_screen"
            ))
        }
        
        binding.buttonCustomEvent.setOnClickListener {
            logEventWithOptions()
        }
    }
    
    private fun initializeSDKs() {
        lifecycleScope.launch {
            try {
                // Test logging for pending events before SDK initialization
                Log.d(TAG, "🧪 [PENDING EVENTS TEST] Starting event logging before SDK initialization...")
                
                monetaiSDK = MonetaiSDK.shared
                
                monetaiSDK.logEvent("app_launched", mapOf(
                    "launch_time" to System.currentTimeMillis() / 1000.0,
                    "version" to "1.0.0",
                    "device" to "Android"
                ))
                
                monetaiSDK.logEvent("app_background_to_foreground", mapOf(
                    "session_start" to true,
                    "user_type" to "example_user"
                ))
                
                monetaiSDK.logEvent("feature_accessed", mapOf(
                    "feature" to "monetai_example",
                    "access_method" to "direct_launch"
                ))
                
                Log.d(TAG, "🧪 [PENDING EVENTS TEST] 3 events logged before SDK initialization")
                Log.d(TAG, "🧪 [PENDING EVENTS TEST] Now initializing SDK to verify pending events are sent...")
                
                // Initialize RevenueCat
                Purchases.configure(PurchasesConfiguration.Builder(this@MainActivity, revenueCatAPIKey).build())
                
                Log.d(TAG, "🚀 [SDK] Starting Monetai SDK initialization...")
                
                // Initialize MonetaiSDK
                monetaiSDK.initialize(this@MainActivity, sdkKey, userId) { result, error ->
                    runOnUiThread {
                        if (error != null) {
                            isInitialized = false
                            updateInitializationStatus("", "❌ Monetai SDK initialization failed\n\n🚨 Error details:\n${error.message}\n\n🔧 Check the following:\n• Verify SDK key is correct\n• Check network connection\n• Check server status")
                            Log.e(TAG, "Failed to initialize SDKs", error)
                        } else {
                            Log.d(TAG, "🎉 [SDK] Monetai SDK initialization complete!")
                            
                            isInitialized = true
                            updateInitializationStatus("✅ Monetai SDK initialization successful!\n\n📊 Initialization result:\n• Organization ID: ${result?.organizationId}\n• Platform: ${result?.platform}\n• Version: ${result?.version}\n• User ID: ${result?.userId}\n• Test Group: ${result?.group ?: "None"}\n\n🎯 Status: Ready\n🧪 Pending Events: 3 events before initialization sent automatically", "")
                            
                            // Log initialization event
                            monetaiSDK.logEvent("monetai_initialized", mapOf(
                                "initialization_time" to System.currentTimeMillis() / 1000.0,
                                "test_group" to (result?.group ?: "none")
                            ))
                            
                            // Load products
                            loadProducts()
                            
                            // Load customer info - user implements directly
                            loadCustomerInfo()
                            
                            // Set up discount info listener
                            setupDiscountInfoListener()
                            
                            Log.d(TAG, "MonetaiSDK initialized successfully: $result")
                        }
                    }
                }
                
            } catch (e: Exception) {
                isInitialized = false
                updateInitializationStatus("", "❌ Monetai SDK initialization failed\n\n🚨 Error details:\n${e.message}\n\n🔧 Check the following:\n• Verify SDK key is correct\n• Check network connection\n• Check server status")
                Log.e(TAG, "Failed to initialize SDKs", e)
            }
        }
    }
    
    private fun setupDiscountInfoListener() {
        monetaiSDK.onDiscountInfoChange = { discount ->
            runOnUiThread {
                if (discount != null) {
                    val isActive = discount.endedAt > Date()
                    val status = if (isActive) {
                        "✅ Active until ${formatDate(discount.endedAt)}"
                    } else {
                        "❌ Expired on ${formatDate(discount.endedAt)}"
                    }
                    updateDiscountStatus(status)
                } else {
                    updateDiscountStatus("No discount available")
                }
            }
        }
    }
    
    // Load RevenueCat products
    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🛍️ [PRODUCTS] Loading products using awaitOfferings...")
                
                val offerings = Purchases.sharedInstance.awaitOfferings()
                
                offerings.current?.availablePackages?.let { availablePackages ->
                    Log.d(TAG, "🛍️ [PRODUCTS] Found ${availablePackages.size} packages")

                    runOnUiThread {
                        productAdapter.updateProducts(availablePackages)
                        updateProductsSection()
                    }
                } ?: run {
                    Log.w(TAG, "🛍️ [PRODUCTS] No current offerings available")
                    runOnUiThread {
                        updateProductsSection()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "🛍️ [PRODUCTS] Failed to load products", e)
                runOnUiThread {
                    updateProductsSection()
                }
            }
        }
    }
    
    // Load RevenueCat customer info
    private fun loadCustomerInfo() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "👤 [CUSTOMER] Loading customer info...")
                
                val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
                
                runOnUiThread {
                    updateCustomerInfo(customerInfo)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "👤 [CUSTOMER] Failed to load customer info", e)
            }
        }
    }
    
    // Handle RevenueCat purchase
    private fun purchasePackage(packageItem: Package) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "💳 [PURCHASE] Starting purchase for package: ${packageItem.identifier}")
                
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(this@MainActivity, packageItem).build()
                )
                
                Log.d(TAG, "✅ [PURCHASE] Purchase successful: ${purchaseResult.customerInfo.entitlements}")
                
                // Log purchase completed event to MonetaiSDK
                monetaiSDK.logEvent("purchase_completed")
                
                runOnUiThread {
                    updateCustomerInfo(purchaseResult.customerInfo)
                    Toast.makeText(this@MainActivity, "Purchase successful!", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [PURCHASE] Purchase failed", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Purchase failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun predictUser() {
        if (isLoading || !isInitialized) return
        
        isLoading = true
        updatePredictButtonState()
        
        monetaiSDK.predict { result, error ->
            runOnUiThread {
                if (error != null) {
                    predictionResult = "Error: ${error.message}"
                } else {
                    predictionResult = result?.prediction?.toString() ?: "Unknown"
                }
                updatePredictionResult()
                isLoading = false
                updatePredictButtonState()
            }
        }
    }
    
    private fun logTestEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        try {
            monetaiSDK.logEvent(eventName, params)
            Log.d(TAG, "✅ [TEST] $eventName event log request")
            Toast.makeText(this@MainActivity, "Event logged: $eventName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $eventName", e)
        }
    }
    
    private fun logEventWithOptions() {
        try {
            val options = LogEventOptions(
                "custom_event",
                mapOf(
                    "category" to "test",
                    "timestamp" to System.currentTimeMillis() / 1000.0,
                    "user_level" to 10
                )
            )
            monetaiSDK.logEvent(options)
            Log.d(TAG, "✅ [TEST] Event log request using LogEventOptions")
            Toast.makeText(this@MainActivity, "Event logged with options", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event with options", e)
        }
    }
    
    private fun logProductViewed(packageItem: Package) {
        if (!isInitialized) return
        
        val productId = packageItem.product.id
        
        val price = packageItem.product.price.amountMicros / 1_000_000.0
        val currencyCode = packageItem.product.price.currencyCode
        val month = packageItem.product.period?.let { period ->
            when (period.unit) {
                Period.Unit.MONTH -> period.value
                Period.Unit.YEAR -> period.value * 12
                else -> null
            }
        }
        
        val params = ViewProductItemParams(
            productId = productId,
            price = price,
            regularPrice = price * 2,
            currencyCode = currencyCode,
            month = month
        )
        
        try {
            monetaiSDK.logViewProductItem(params)
            Log.d(TAG, "✅ [PRODUCT VIEW] Logged product view for $productId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log product view for $productId", e)
        }
    }
    
    // UI Update Methods
    private fun updateInitializationStatus(success: String, error: String) {
        binding.textViewInitializationResult.text = success
        binding.textViewInitializationError.text = error
        binding.textViewInitializationResult.visibility = if (success.isNotEmpty()) View.VISIBLE else View.GONE
        binding.textViewInitializationError.visibility = if (error.isNotEmpty()) View.VISIBLE else View.GONE
        updateStatusIndicator()
    }
    
    private fun updateStatusIndicator() {
        binding.imageViewStatus.setImageResource(
            if (isInitialized) android.R.drawable.presence_online
            else android.R.drawable.presence_offline
        )
        binding.textViewStatus.text = if (isInitialized) "Connected" else "Connecting..."
        
        // Update SDK status details in the bottom section
        updateSDKStatusDetails()
    }
    
    private fun updateSDKStatusDetails() {
        // Update initialized status
        binding.textViewInitializedStatus.text = "🔧 Initialized: ${if (isInitialized) "✅ YES" else "❌ NO"}"
        
        // Update server status
        binding.textViewServerStatus.text = "🌐 Server: ${if (isInitialized) "🟢 Connected" else "🔴 Disconnected"}"
        
        // Update SDK key display (show first 8 characters)
        val sdkKeyDisplay = if (sdkKey.length > 8) "${sdkKey.substring(0, 8)}..." else sdkKey
        binding.textViewSdkKey.text = "🔑 SDK Key: $sdkKeyDisplay"
        
        // Update user ID display
        binding.textViewUserId.text = "👤 User ID: $userId"
    }
    
    private fun updateProductsSection() {
        // Update products section with proper loading states
        binding.progressBarProducts.visibility = View.GONE
        binding.textViewProductsLoading.visibility = View.GONE
        
        if (productAdapter.itemCount > 0) {
            binding.recyclerViewProducts.visibility = View.VISIBLE
            Log.d(TAG, "🛍️ [PRODUCTS] Displaying ${productAdapter.itemCount} products")
        } else {
            binding.recyclerViewProducts.visibility = View.GONE
            Log.d(TAG, "🛍️ [PRODUCTS] No products available to display")
        }
    }
    
    private fun updateCustomerInfo(customerInfo: CustomerInfo?) {
        if (customerInfo != null) {
            val activeEntitlements = customerInfo.entitlements.active
            
            if (activeEntitlements.isNotEmpty()) {
                // When there are active entitlements
                binding.textViewNoSubscriptions.visibility = View.GONE
                binding.recyclerViewEntitlements.visibility = View.VISIBLE
                
                // Update entitlements adapter with active entitlements
                val entitlementsList = activeEntitlements.values.toList()
                entitlementAdapter.updateEntitlements(entitlementsList)
                
                Log.d(TAG, "👤 [CUSTOMER] Active entitlements: ${activeEntitlements.keys.joinToString()}")
                Log.d(TAG, "👤 [CUSTOMER] Displaying ${entitlementsList.size} entitlements")
                
            } else {
                // When there are no active entitlements
                binding.textViewNoSubscriptions.visibility = View.VISIBLE
                binding.recyclerViewEntitlements.visibility = View.GONE
                Log.d(TAG, "👤 [CUSTOMER] No active entitlements found")
            }
        } else {
            // CustomerInfo load failed
            binding.textViewNoSubscriptions.visibility = View.VISIBLE
            binding.recyclerViewEntitlements.visibility = View.GONE
            Log.w(TAG, "👤 [CUSTOMER] Customer info is null")
        }
    }
    
    private fun updateDiscountStatus(status: String) {
        binding.textViewDiscountStatus.text = status
    }
    
    private fun updatePredictionResult() {
        binding.textViewPredictionResult.text = predictionResult
        binding.textViewPredictionResult.visibility = if (predictionResult.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun updatePredictButtonState() {
        binding.buttonPredictUser.isEnabled = !isLoading && isInitialized
        binding.progressBarPredict.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
} 