package com.monetai.sample.java;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monetai.sample.java.databinding.ActivityMainBinding;
import com.monetai.sdk.MonetaiSDKJava;
import com.monetai.sdk.models.AppUserDiscount;
import com.monetai.sdk.models.PredictResult;
import com.monetai.sdk.models.PaywallConfig;
import com.monetai.sdk.models.PaywallStyle;
import com.monetai.sdk.models.Feature;
import com.monetai.sdk.models.ViewProductItemParams;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Arrays;
import java.util.List;



public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // MARK: - Paywall State
    private PaywallConfig paywallConfig;
    
    // MARK: - Fake Products
    private FakeProductAdapter fakeProductAdapter;
    private final List<FakeProduct> fakeProducts = Arrays.asList(
        new FakeProduct(
            "fake_basic_monthly",
            "Basic Plan",
            "Essential features for starters",
            4.99,
            9.98,
            "USD",
            1
        ),
        new FakeProduct(
            "fake_pro_yearly",
            "Pro Plan",
            "Advanced tools for power users",
            59.99,
            119.98,
            "USD",
            12
        )
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupUI();
        
        // Log app launch event BEFORE SDK initialization
        executor.execute(() -> MonetaiSDKJava.getShared().logEvent("app_in"));
        
        setupMonetaiSDK();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
    
    private void setupUI() {
        // Setup button click listeners
        binding.predictButton.setOnClickListener(v -> predictButtonTapped());
        binding.logEventButton.setOnClickListener(v -> logEventButtonTapped());
        
        // Fake products list
        fakeProductAdapter = new FakeProductAdapter(this::logFakeProductViewed);
        binding.recyclerViewFakeProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewFakeProducts.setAdapter(fakeProductAdapter);
    }
    
    private void setupMonetaiSDK() {
        System.out.println("🎯 Setting up Monetai SDK...");
        
        MonetaiSDKJava.getShared().initialize(
            this,
            Constants.SDK_KEY, // SDK Key
            Constants.USER_ID, // User ID
            (result, error) -> {
                if (error != null) {
                    Log.e(TAG, "SDK initialization failed", error);
                    
                    // Update status to show initialization failed
                    runOnUiThread(() -> {
                        binding.statusLabel.setText("SDK Status: ❌ Failed to Initialize");
                        binding.statusLabel.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                    });
                } else {
                    Log.d(TAG, "SDK initialized successfully: " + result);
                    
                    // Enable buttons and update status after successful initialization
                    runOnUiThread(() -> {
                        binding.statusLabel.setText("SDK Status: ✅ Initialized");
                        binding.statusLabel.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_dark));
                        binding.predictButton.setEnabled(true);
                        binding.logEventButton.setEnabled(true);
                        
                         // Load fake products after initialization for logging
                        loadFakeProducts();
                    });
                    
                    // Set discount info change callback AFTER initialization
                    // This should now work regardless of timing
                    MonetaiSDKJava.getShared().setOnDiscountInfoChange(discount -> {
                        Log.d(TAG, "Discount info changed: " + discount);
                        handleDiscountInfoChange(discount);
                    });
                    
                    // Set up paywall AFTER initialization
                    setupPaywall();
                }
            }
        );
    }
    
    
    private void handleDiscountInfoChange(AppUserDiscount discountInfo) {
        runOnUiThread(() -> {
            if (discountInfo != null) {
                Date now = new Date();
                Date endTime = discountInfo.getEndedAt();

                // Debug time comparison
                System.out.println("🔍 Time Debug:");
                System.out.println("  Current time (local): " + now);
                System.out.println("  End time (from API): " + endTime);
                System.out.println("  Time comparison: now < endTime = " + (now.before(endTime)));
                System.out.println("  Time difference (ms): " + (endTime.getTime() - now.getTime()));

                if (now.before(endTime)) {
                    // Discount is valid - SDK banner will be shown automatically
                    binding.discountStatusLabel.setText("Discount: ✅ Active (Expires: " + dateFormat.format(endTime) + ")");
                    binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                } else {
                    // Discount expired
                    binding.discountStatusLabel.setText("Discount: ❌ Expired");
                    binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                }
            } else {
                // No discount
                binding.discountStatusLabel.setText("Discount: None");
                binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }
        });
    }


    // MARK: - Button Actions
    private void predictButtonTapped() {
        executor.execute(() -> {
            try {
                MonetaiSDKJava.getShared().predict((result, error) -> {
                    if (error != null) {
                        System.out.println("Prediction failed: " + error);
                        runOnUiThread(() -> {
                            binding.resultLabel.setText("❌ Prediction failed: " + error.getMessage());
                            binding.resultLabel.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                        });
                    } else {
                        // Check if result is null before accessing its methods
                        if (result != null) {
                            System.out.println("Prediction result: " + result.getPrediction());
                            System.out.println("Test group: " + result.getTestGroup());

                            if (result.getPrediction() != null) {
                                PredictResult prediction = result.getPrediction();
                                if (prediction == PredictResult.NON_PURCHASER) {
                                    // When predicted as non-purchaser, SDK should automatically show paywall
                                    System.out.println("Predicted as non-purchaser - SDK should automatically show paywall");
                                    System.out.println("Note: If paywall doesn't appear, check SDK's automatic display logic");
                                } else if (prediction == PredictResult.PURCHASER) {
                                    // When predicted as purchaser
                                    System.out.println("Predicted as purchaser - paywall not needed");
                                }
                            } else {
                                // When prediction is null
                                System.out.println("No prediction available");
                            }

                            runOnUiThread(() -> {
                                binding.resultLabel.setText("✅ Prediction completed - check console for details");
                                binding.resultLabel.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_dark));

                                // Show alert with prediction result
                                new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Purchase Prediction")
                                    .setMessage("Prediction: " + result.getPrediction() + "\nTest Group: " + result.getTestGroup())
                                    .setPositiveButton("OK", null)
                                    .show();
                            });
                        } else {
                            System.out.println("Prediction result is null");
                            runOnUiThread(() -> {
                                binding.resultLabel.setText("❌ Prediction result is null");
                                binding.resultLabel.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                            });
                        }
                    }
                });

            } catch (Exception error) {
                runOnUiThread(() -> {
                    binding.resultLabel.setText("❌ Prediction failed: " + error.getMessage());
                    binding.resultLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                });
                System.out.println("Prediction failed: " + error);
            }
        });
    }

    private void logEventButtonTapped() {
        executor.execute(() -> {
            // Log a sample event with parameters
            Map<String, Object> params = new HashMap<>();
            params.put("button", "test_button");
            params.put("screen", "main");
            
            MonetaiSDKJava.getShared().logEvent("button_click", params);

            runOnUiThread(() -> {
                binding.resultLabel.setText("✅ Event logged: button_click\nParameters: button=test_button, screen=main");
                binding.resultLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            });

            System.out.println("Event logged: button_click");
        });
    }
    
    // MARK: - Fake Products
    private void loadFakeProducts() {
        fakeProductAdapter.submitList(fakeProducts);
    }
    
    private void logFakeProductViewed(FakeProduct product) {
        if (!MonetaiSDKJava.getInitialized()) return;
        
        ViewProductItemParams params = new ViewProductItemParams(
            product.id,
            product.price,
            product.regularPrice,
            product.currencyCode,
            product.month
        );
        
        executor.execute(() -> {
            try {
                MonetaiSDKJava.getShared().logViewProductItem(params);
                Log.d(TAG, "✅ Logged fake product view: " + product.id);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log fake product view: " + product.id, e);
            }
        });
    }
    
    // MARK: - Paywall Methods
    private void setupPaywall() {
        paywallConfig = MonetaiSDKJava.createPaywallConfigBuilder()
            .discountPercent(30)
            .regularPrice("$99.99")
            .discountedPrice("$69.99")
            .locale("en")
            .style(PaywallStyle.COMPACT)
            .features(Arrays.asList(
                new Feature("Unlimited Access", "Use all premium features without limits", true),
                new Feature("Advanced Analytics", "AI-powered insights", false),
                new Feature("Priority Support", "24/7 customer support", false)
            ))
            .enabled(true)
            .bannerBottom(20f)
            .isSubscriber(false)
            .onPurchase(new MonetaiSDKJava.OnPurchaseCallback() {
                @Override
                public void onPurchase(com.monetai.sdk.models.PaywallContext paywallContext, Runnable closePaywall) {
                    System.out.println("🛒 Purchase button tapped in paywall!");
                    System.out.println("  📱 Activity: " + paywallContext.getActivity().getClass().getSimpleName());
                    System.out.println("  🏗️ App Context: " + paywallContext.getApplicationContext().getClass().getSimpleName());
                    
                    runOnUiThread(() -> {
                        // Simulate subscription purchase
                        MonetaiSDKJava.setSubscriptionStatus(true);
                        
                        // Show success toast
                        Toast.makeText(MainActivity.this, "🎉 Purchase successful! Welcome to Premium!", Toast.LENGTH_SHORT).show();
                    });
                    
                    // Close paywall
                    closePaywall.run();
                }
            })
            .onTermsOfService(new MonetaiSDKJava.OnTermsOfServiceCallback() {
                @Override
                public void onTermsOfService(com.monetai.sdk.models.PaywallContext paywallContext) {
                    System.out.println("📄 Terms of Service tapped!");
                    System.out.println("  📱 Activity: " + paywallContext.getActivity().getClass().getSimpleName());
                    showTermsOfService(paywallContext);
                }
            })
            .onPrivacyPolicy(new MonetaiSDKJava.OnPrivacyPolicyCallback() {
                @Override
                public void onPrivacyPolicy(com.monetai.sdk.models.PaywallContext paywallContext) {
                    System.out.println("🔒 Privacy Policy tapped!");
                    System.out.println("  📱 Activity: " + paywallContext.getActivity().getClass().getSimpleName());
                    showPrivacyPolicy(paywallContext);
                }
            })
            .build();
        
        if (paywallConfig != null) {
            // Configure paywall
            MonetaiSDKJava.configurePaywall(paywallConfig);
            System.out.println("🎯 Paywall configured successfully");
        }
    }
    
    // MARK: - Paywall Callback Helpers
    private static void showTermsOfService(com.monetai.sdk.models.PaywallContext paywallContext) {
        System.out.println("📄 Showing Terms of Service...");
        System.out.println("  🎯 Context from: " + paywallContext.getActivity().getClass().getSimpleName());
        
        // Use the Activity from PaywallContext for better dialog management
        android.app.Activity contextActivity = paywallContext.getActivity();
        
        // PaywallContext의 Activity는 이미 MonetaiPaywallActivity이므로 직접 사용
        contextActivity.runOnUiThread(() -> {
            String contextInfo = "Called from: " + paywallContext.getActivity().getClass().getSimpleName();
            
            // 가장 간단한 기본 다이얼로그 - PaywallActivity에서 직접 표시
            new AlertDialog.Builder(contextActivity)
                .setTitle("Terms of Service")
                .setMessage("Terms of Service content goes here...\n\n" + contextInfo)
                .setPositiveButton("Accept", null)
                .setNegativeButton("Cancel", null)
                .show();
            
            System.out.println("✅ Terms of Service dialog shown on PaywallActivity: " + contextActivity.getClass().getSimpleName());
        });
    }
    
    private static void showPrivacyPolicy(com.monetai.sdk.models.PaywallContext paywallContext) {
        System.out.println("🔒 Showing Privacy Policy...");
        System.out.println("  🎯 Context from: " + paywallContext.getActivity().getClass().getSimpleName());
        
        // Use the Activity from PaywallContext for better dialog management
        android.app.Activity contextActivity = paywallContext.getActivity();
        
        // PaywallContext의 Activity는 이미 MonetaiPaywallActivity이므로 직접 사용
        contextActivity.runOnUiThread(() -> {
            String contextInfo = "Called from: " + paywallContext.getActivity().getClass().getSimpleName();
            
            // 가장 간단한 기본 다이얼로그 - PaywallActivity에서 직접 표시
            new AlertDialog.Builder(contextActivity)
                .setTitle("Privacy Policy")
                .setMessage("Privacy Policy content goes here...\n\n" + contextInfo)
                .setPositiveButton("Accept", null)
                .setNegativeButton("Cancel", null)
                .show();
            
            System.out.println("✅ Privacy Policy dialog shown on PaywallActivity: " + contextActivity.getClass().getSimpleName());
        });
    }
} 