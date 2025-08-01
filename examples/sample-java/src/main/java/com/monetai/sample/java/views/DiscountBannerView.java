package com.monetai.sample.java.views;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.monetai.sample.java.R;
import com.monetai.sdk.models.AppUserDiscount;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class DiscountBannerView extends ConstraintLayout {

    // MARK: - UI Elements
    private CardView containerView;
    private TextView titleLabel;
    private TextView descriptionLabel;
    private TextView timeRemainingLabel;
    private Button closeButton;

    // MARK: - Properties
    private AppUserDiscount discount;
    private Timer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    // MARK: - Initialization
    public DiscountBannerView(Context context) {
        super(context);
        setupUI();
    }

    public DiscountBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupUI();
    }

    public DiscountBannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupUI();
    }

    // MARK: - UI Setup
    private void setupUI() {
        // Inflate layout
        LayoutInflater.from(getContext()).inflate(R.layout.view_discount_banner, this, true);

        // Initialize views
        containerView = findViewById(R.id.containerView);
        titleLabel = findViewById(R.id.titleLabel);
        descriptionLabel = findViewById(R.id.descriptionLabel);
        timeRemainingLabel = findViewById(R.id.timeRemainingLabel);
        closeButton = findViewById(R.id.closeButton);

        // Setup close button
        closeButton.setOnClickListener(v -> hideDiscount());

        // Set initial visibility
        setVisibility(View.GONE);
    }

    // MARK: - Public Methods
    public void showDiscount(AppUserDiscount discount) {
        System.out.println("🎯 DiscountBannerView.showDiscount called");
        System.out.println("  discount: " + discount);
        
        this.discount = discount;
        updateTimeRemaining();
        startTimer();

        // Set text
        titleLabel.setText("🎉 Special Discount!");
        descriptionLabel.setText("Limited time offer available for you!");

        // Debug logging
        System.out.println("🎯 DiscountBannerView Debug:");
        System.out.println("  Title text: " + titleLabel.getText());
        System.out.println("  Description text: " + descriptionLabel.getText());
        System.out.println("  Time remaining text: " + timeRemainingLabel.getText());
        System.out.println("  Visibility before: " + getVisibility());
        System.out.println("  Alpha before: " + getAlpha());
        System.out.println("  Parent: " + getParent());
        System.out.println("  Layout params: " + getLayoutParams());

        // Animate in
        setVisibility(View.VISIBLE);
        setAlpha(0f);
        setTranslationY(50f);

        System.out.println("  Visibility after setting VISIBLE: " + getVisibility());
        System.out.println("  Alpha after setting 0f: " + getAlpha());

        animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    System.out.println("  Animation completed");
                    System.out.println("  Final visibility: " + getVisibility());
                    System.out.println("  Final alpha: " + getAlpha());
                })
                .start();
    }

    public void hideDiscount() {
        stopTimer();

        // Animate out
        animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(300)
                .withEndAction(() -> {
                    setVisibility(View.GONE);
                    if (getParent() instanceof android.view.ViewGroup) {
                        ((android.view.ViewGroup) getParent()).removeView(this);
                    }
                })
                .start();
    }

    // MARK: - Private Methods
    private void updateTimeRemaining() {
        if (discount == null) return;

        Date now = new Date();
        Date endTime = discount.getEndedAt();
        long timeRemaining = endTime.getTime() - now.getTime();

        if (timeRemaining > 0) {
            int hours = (int) (timeRemaining / (1000 * 60 * 60));
            int minutes = (int) ((timeRemaining % (1000 * 60 * 60)) / (1000 * 60));

            String timeText;
            if (hours > 0) {
                timeText = "⏰ " + hours + "h " + minutes + "m remaining";
            } else {
                timeText = "⏰ " + minutes + "m remaining";
            }
            timeRemainingLabel.setText(timeText);
        } else {
            timeRemainingLabel.setText("⏰ Expired");
            hideDiscount();
        }
    }

    private void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> updateTimeRemaining());
            }
        }, 60000, 60000); // Update every minute
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
} 