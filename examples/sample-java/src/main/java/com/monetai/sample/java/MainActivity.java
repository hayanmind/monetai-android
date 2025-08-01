package com.monetai.sample.java;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.monetai.sample.java.databinding.ActivityMainBinding;
import com.monetai.sample.java.views.DiscountBannerView;
import com.monetai.sdk.MonetaiSDKJava;
import com.monetai.sdk.models.AppUserDiscount;
import com.monetai.sdk.models.PredictResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private DiscountBannerView discountBannerView;
    private static final String TAG = "MainActivity";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
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
                    updateStatus("SDK initialization failed: " + error.getMessage());
                } else {
                    Log.d(TAG, "SDK initialized successfully: " + result);
                    updateStatus("SDK Status: ✅ Ready");
                    
                    // Enable buttons after successful initialization
                    runOnUiThread(() -> {
                        binding.predictButton.setEnabled(true);
                        binding.logEventButton.setEnabled(true);
                    });
                    
                    // Set discount info change callback AFTER initialization
                    // This should now work regardless of timing
                    MonetaiSDKJava.getShared().setOnDiscountInfoChange(discount -> {
                        Log.d(TAG, "Discount info changed: " + discount);
                        handleDiscountInfoChange(discount);
                    });
                }
            }
        );
    }
    
    private void updateStatus(String message) {
        runOnUiThread(() -> {
            binding.statusLabel.setText(message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
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
                    // Discount is valid - show banner
                    binding.discountStatusLabel.setText("Discount: ✅ Active (Expires: " + dateFormat.format(endTime) + ")");
                    binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                    showDiscountBanner(discountInfo);
                } else {
                    // Discount expired
                    binding.discountStatusLabel.setText("Discount: ❌ Expired");
                    binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                    hideDiscountBanner();
                }
            } else {
                // No discount
                binding.discountStatusLabel.setText("Discount: None");
                binding.discountStatusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                hideDiscountBanner();
            }
        });
    }

    private void showDiscountBanner(AppUserDiscount discount) {
        // Remove existing banner if any
        hideDiscountBanner();

        // Create and add new banner
        discountBannerView = new DiscountBannerView(this);
        
        // Debug logging
        System.out.println("🎯 showDiscountBanner called");
        System.out.println("  discountBannerView created: " + (discountBannerView != null));
        System.out.println("  rootLayout: " + binding.getRoot());
        
        // Add banner to root layout
        binding.getRoot().addView(discountBannerView);
        
        // Set layout parameters to position at bottom
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams layoutParams = 
            new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
        layoutParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.bottomMargin = 20;
        layoutParams.leftMargin = 16;
        layoutParams.rightMargin = 16;
        discountBannerView.setLayoutParams(layoutParams);
        
        System.out.println("  Banner added to rootLayout");
        System.out.println("  Banner visibility before showDiscount: " + discountBannerView.getVisibility());

        // Show discount
        discountBannerView.showDiscount(discount);

        // Update result label
        binding.resultLabel.setText("🎉 Discount banner displayed!\nSpecial offer is now active.");
        binding.resultLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        
        System.out.println("  showDiscountBanner completed");
    }

    private void hideDiscountBanner() {
        if (discountBannerView != null) {
            discountBannerView.hideDiscount();
            discountBannerView = null;
        }
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
                                    // When predicted as non-purchaser, offer discount
                                    System.out.println("Predicted as non-purchaser - discount can be applied");
                                } else if (prediction == PredictResult.PURCHASER) {
                                    // When predicted as purchaser
                                    System.out.println("Predicted as purchaser - discount not needed");
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
} 