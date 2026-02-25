package com.monetai.sample.kotlin.purchase;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.monetai.sample.kotlin.purchase.databinding.ActivityMainBinding;
import com.monetai.sdk.MonetaiSDKJava;
import com.monetai.sdk.models.LogEventOptions;
import com.monetai.sdk.models.Offer;
import com.monetai.sdk.models.OfferProduct;
import com.monetai.sdk.models.ViewProductItemParams;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchaseParams;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.models.StoreTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private MonetaiSDKJava monetaiSDK;
    private ProductAdapter productAdapter;
    private EntitlementAdapter entitlementAdapter;

    private boolean isInitialized = false;
    private String offerResult = "";
    private boolean isLoading = false;

    private Offer offer = null;
    private List<Package> allPackages = new ArrayList<>();

    private final String sdkKey = Constants.SDK_KEY;
    private final String userId = Constants.USER_ID;
    private final String revenueCatAPIKey = Constants.REVENUE_CAT_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        updateSDKStatusDetails();
        initializeSDKs();
    }

    private void setupUI() {
        // Set up RecyclerView for products
        productAdapter = new ProductAdapter(this::purchasePackage);
        binding.recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewProducts.setAdapter(productAdapter);

        // Set up RecyclerView for entitlements
        entitlementAdapter = new EntitlementAdapter();
        binding.recyclerViewEntitlements.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewEntitlements.setAdapter(entitlementAdapter);

        // Set up click listeners
        binding.buttonPredictUser.setOnClickListener(v -> getOffer());

        binding.buttonAppOpened.setOnClickListener(v -> logTestEvent("app_opened", null));

        binding.buttonProductViewed.setOnClickListener(v -> logTestEvent("product_viewed", null));

        binding.buttonAddToCart.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("value", 9.99);
            logTestEvent("add_to_cart", params);
        });

        binding.buttonPurchaseStarted.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("value", 19.99);
            logTestEvent("purchase_started", params);
        });

        binding.buttonScreenView.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("screen_name", "home");
            params.put("previous_screen", "onboarding");
            logTestEvent("screen_view", params);
        });

        binding.buttonButtonClick.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("button_name", "upgrade");
            params.put("button_location", "home_screen");
            logTestEvent("button_click", params);
        });

        binding.buttonFeatureUsed.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("feature", "monetai_example");
            logTestEvent("feature_used", params);
        });

        binding.buttonCustomEvent.setOnClickListener(v -> logEventWithOptions());
    }

    private void initializeSDKs() {
        Log.d(TAG, "[PENDING EVENTS TEST] Starting event logging before SDK initialization...");

        monetaiSDK = MonetaiSDKJava.getShared();

        // Log events before initialization to test pending events queue
        Map<String, Object> launchParams = new HashMap<>();
        launchParams.put("launch_time", System.currentTimeMillis() / 1000.0);
        launchParams.put("version", "1.0.0");
        launchParams.put("device", "Android");
        monetaiSDK.logEvent("app_launched", launchParams);

        Map<String, Object> foregroundParams = new HashMap<>();
        foregroundParams.put("session_start", true);
        foregroundParams.put("user_type", "example_user");
        monetaiSDK.logEvent("app_background_to_foreground", foregroundParams);

        Map<String, Object> featureParams = new HashMap<>();
        featureParams.put("feature", "monetai_example");
        featureParams.put("access_method", "direct_launch");
        monetaiSDK.logEvent("feature_accessed", featureParams);

        Log.d(TAG, "[PENDING EVENTS TEST] 3 events logged before SDK initialization");
        Log.d(TAG, "[PENDING EVENTS TEST] Now initializing SDK to verify pending events are sent...");

        // Initialize RevenueCat
        Purchases.configure(new PurchasesConfiguration.Builder(this, revenueCatAPIKey).build());

        Log.d(TAG, "[SDK] Starting Monetai SDK initialization...");

        // Initialize MonetaiSDK
        monetaiSDK.initialize(this, sdkKey, userId, (result, error) -> {
            runOnUiThread(() -> {
                if (error != null) {
                    isInitialized = false;
                    updateInitializationStatus("",
                            "Monetai SDK initialization failed\n\nError details:\n" + error.getMessage() +
                                    "\n\nCheck the following:\n- Verify SDK key is correct\n- Check network connection\n- Check server status");
                    Log.e(TAG, "Failed to initialize SDKs", error);
                } else {
                    Log.d(TAG, "[SDK] Monetai SDK initialization complete!");

                    isInitialized = true;
                    String successMessage = "Monetai SDK initialization successful!\n\nInitialization result:\n" +
                            "- Organization ID: " + (result != null ? result.getOrganizationId() : "N/A") + "\n" +
                            "- Platform: " + (result != null ? result.getPlatform() : "N/A") + "\n" +
                            "- Version: " + (result != null ? result.getVersion() : "N/A") + "\n" +
                            "- User ID: " + (result != null ? result.getUserId() : "N/A") + "\n" +
                            "\nStatus: Ready\nPending Events: 3 events before initialization sent automatically";
                    updateInitializationStatus(successMessage, "");

                    // Log initialization event
                    Map<String, Object> initParams = new HashMap<>();
                    initParams.put("initialization_time", System.currentTimeMillis() / 1000.0);
                    monetaiSDK.logEvent("monetai_initialized", initParams);

                    // Load products
                    loadProducts();

                    // Load customer info
                    loadCustomerInfo();

                    Log.d(TAG, "MonetaiSDK initialized successfully: " + result);
                }
            });
        });
    }

    // Load RevenueCat products
    private void loadProducts() {
        Log.d(TAG, "[PRODUCTS] Loading products using getOfferings...");

        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                List<Package> availablePackages = offerings.getCurrent() != null
                        ? offerings.getCurrent().getAvailablePackages()
                        : null;

                if (availablePackages != null && !availablePackages.isEmpty()) {
                    Log.d(TAG, "[PRODUCTS] Found " + availablePackages.size() + " packages");
                    runOnUiThread(() -> {
                        allPackages = availablePackages;
                        updateDisplayedProducts();
                    });
                } else {
                    Log.w(TAG, "[PRODUCTS] No current offerings available");
                    runOnUiThread(() -> updateProductsSection());
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e(TAG, "[PRODUCTS] Failed to load products: " + error.getMessage());
                runOnUiThread(() -> updateProductsSection());
            }
        });
    }

    // Load RevenueCat customer info
    private void loadCustomerInfo() {
        Log.d(TAG, "[CUSTOMER] Loading customer info...");

        Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo customerInfo) {
                runOnUiThread(() -> updateCustomerInfo(customerInfo));
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e(TAG, "[CUSTOMER] Failed to load customer info: " + error.getMessage());
            }
        });
    }

    // Handle RevenueCat purchase
    private void purchasePackage(Package packageItem) {
        Log.d(TAG, "[PURCHASE] Starting purchase for package: " + packageItem.getIdentifier());

        Purchases.getSharedInstance().purchase(
                new PurchaseParams.Builder(this, packageItem).build(),
                new PurchaseCallback() {
                    @Override
                    public void onCompleted(@NonNull StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) {
                        Log.d(TAG, "[PURCHASE] Purchase successful: " + customerInfo.getEntitlements());

                        // Log purchase completed event to MonetaiSDK
                        monetaiSDK.logEvent("purchase_completed");

                        runOnUiThread(() -> {
                            updateCustomerInfo(customerInfo);
                            Toast.makeText(MainActivity.this, "Purchase successful!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                        Log.e(TAG, "[PURCHASE] Purchase failed: " + error.getMessage());
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Purchase failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
        );
    }

    private void getOffer() {
        if (isLoading || !isInitialized) return;

        isLoading = true;
        updatePredictButtonState();

        monetaiSDK.getOffer(Constants.PROMOTION_ID, (offer, error) -> {
            runOnUiThread(() -> {
                if (error != null) {
                    offerResult = "Error: " + error.getMessage();
                } else if (offer != null) {
                    this.offer = offer;
                    offerResult = "Agent: " + offer.getAgentName() + "\nProducts: " + offer.getProducts().size();

                    // Update product display with offer info
                    updateDisplayedProducts();

                    // Log view product item for each offer product
                    logViewProductItems(offer);
                } else {
                    offerResult = "No offer available";
                }
                updatePredictionResult();
                isLoading = false;
                updatePredictButtonState();
            });
        });
    }

    private String toOfferSku(String identifier) {
        return identifier.replace(":base", "");
    }

    private Package findBasePackage() {
        for (Package pkg : allPackages) {
            if (toOfferSku(pkg.getProduct().getId()).equals(Constants.DEFAULT_PRODUCT_ID)) {
                return pkg;
            }
        }
        return null;
    }

    private void updateDisplayedProducts() {
        Package basePackage = findBasePackage();

        List<Package> displayedPackages;
        if (basePackage == null) {
            displayedPackages = new ArrayList<>();
        } else if (offer != null) {
            Set<String> offerSkus = new HashSet<>();
            for (OfferProduct product : offer.getProducts()) {
                offerSkus.add(product.getSku());
            }

            displayedPackages = new ArrayList<>();
            displayedPackages.add(basePackage);
            for (Package pkg : allPackages) {
                if (offerSkus.contains(toOfferSku(pkg.getProduct().getId()))) {
                    displayedPackages.add(pkg);
                }
            }
        } else {
            displayedPackages = new ArrayList<>();
            displayedPackages.add(basePackage);
        }

        productAdapter.updateProducts(displayedPackages, offer, basePackage);
        updateProductsSection();
    }

    private void logViewProductItems(Offer offer) {
        Package basePackage = findBasePackage();
        if (basePackage == null) return;

        for (OfferProduct offerProduct : offer.getProducts()) {
            Package matchedPkg = null;
            for (Package pkg : allPackages) {
                if (toOfferSku(pkg.getProduct().getId()).equals(offerProduct.getSku())) {
                    matchedPkg = pkg;
                    break;
                }
            }
            if (matchedPkg != null) {
                monetaiSDK.logViewProductItem(
                        new ViewProductItemParams(
                                matchedPkg.getProduct().getId(),
                                matchedPkg.getProduct().getPrice().getAmountMicros() / 1_000_000.0,
                                basePackage.getProduct().getPrice().getAmountMicros() / 1_000_000.0,
                                matchedPkg.getProduct().getPrice().getCurrencyCode(),
                                Constants.PROMOTION_ID,
                                null
                        )
                );
            }
        }
    }

    private void logTestEvent(String eventName, Map<String, Object> params) {
        try {
            if (params != null) {
                monetaiSDK.logEvent(eventName, params);
            } else {
                monetaiSDK.logEvent(eventName);
            }
            Log.d(TAG, "[TEST] " + eventName + " event log request");
            Toast.makeText(this, "Event logged: " + eventName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to log event: " + eventName, e);
        }
    }

    private void logEventWithOptions() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("category", "test");
            params.put("timestamp", System.currentTimeMillis() / 1000.0);
            params.put("user_level", 10);

            LogEventOptions options = new LogEventOptions("custom_event", params, new Date());
            monetaiSDK.logEvent(options);
            Log.d(TAG, "[TEST] Event log request using LogEventOptions");
            Toast.makeText(this, "Event logged with options", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to log event with options", e);
        }
    }

    // UI Update Methods
    private void updateInitializationStatus(String success, String error) {
        binding.textViewInitializationResult.setText(success);
        binding.textViewInitializationError.setText(error);
        binding.textViewInitializationResult.setVisibility(!success.isEmpty() ? View.VISIBLE : View.GONE);
        binding.textViewInitializationError.setVisibility(!error.isEmpty() ? View.VISIBLE : View.GONE);
        updateStatusIndicator();
    }

    private void updateStatusIndicator() {
        binding.imageViewStatus.setImageResource(
                isInitialized ? android.R.drawable.presence_online : android.R.drawable.presence_offline
        );
        binding.textViewStatus.setText(isInitialized ? "Connected" : "Connecting...");
        updateSDKStatusDetails();
    }

    private void updateSDKStatusDetails() {
        binding.textViewInitializedStatus.setText("Initialized: " + (isInitialized ? "YES" : "NO"));
        binding.textViewServerStatus.setText("Server: " + (isInitialized ? "Connected" : "Disconnected"));

        String sdkKeyDisplay = sdkKey.length() > 8 ? sdkKey.substring(0, 8) + "..." : sdkKey;
        binding.textViewSdkKey.setText("SDK Key: " + sdkKeyDisplay);
        binding.textViewUserId.setText("User ID: " + userId);
    }

    private void updateProductsSection() {
        binding.progressBarProducts.setVisibility(View.GONE);
        binding.textViewProductsLoading.setVisibility(View.GONE);

        if (productAdapter.getItemCount() > 0) {
            binding.recyclerViewProducts.setVisibility(View.VISIBLE);
            Log.d(TAG, "[PRODUCTS] Displaying " + productAdapter.getItemCount() + " products");
        } else {
            binding.recyclerViewProducts.setVisibility(View.GONE);
            Log.d(TAG, "[PRODUCTS] No products available to display");
        }
    }

    private void updateCustomerInfo(CustomerInfo customerInfo) {
        if (customerInfo != null) {
            Map<String, EntitlementInfo> activeEntitlements = customerInfo.getEntitlements().getActive();

            if (!activeEntitlements.isEmpty()) {
                binding.textViewNoSubscriptions.setVisibility(View.GONE);
                binding.recyclerViewEntitlements.setVisibility(View.VISIBLE);

                List<EntitlementInfo> entitlementsList = new ArrayList<>(activeEntitlements.values());
                entitlementAdapter.updateEntitlements(entitlementsList);

                Log.d(TAG, "[CUSTOMER] Active entitlements: " + activeEntitlements.keySet());
                Log.d(TAG, "[CUSTOMER] Displaying " + entitlementsList.size() + " entitlements");
            } else {
                binding.textViewNoSubscriptions.setVisibility(View.VISIBLE);
                binding.recyclerViewEntitlements.setVisibility(View.GONE);
                Log.d(TAG, "[CUSTOMER] No active entitlements found");
            }
        } else {
            binding.textViewNoSubscriptions.setVisibility(View.VISIBLE);
            binding.recyclerViewEntitlements.setVisibility(View.GONE);
            Log.w(TAG, "[CUSTOMER] Customer info is null");
        }
    }

    private void updateDiscountStatus(String status) {
        binding.textViewDiscountStatus.setText(status);
    }

    private void updatePredictionResult() {
        binding.textViewPredictionResult.setText(offerResult);
        binding.textViewPredictionResult.setVisibility(!offerResult.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updatePredictButtonState() {
        binding.buttonPredictUser.setEnabled(!isLoading && isInitialized);
        binding.progressBarPredict.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return formatter.format(date);
    }
}
