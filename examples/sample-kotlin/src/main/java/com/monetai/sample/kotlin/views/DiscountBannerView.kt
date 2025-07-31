package com.monetai.sample.kotlin.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.monetai.sample.kotlin.R
import com.monetai.sdk.models.AppUserDiscount
import java.text.SimpleDateFormat
import java.util.*

class DiscountBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // MARK: - UI Elements
    private lateinit var containerView: CardView
    private lateinit var titleLabel: TextView
    private lateinit var descriptionLabel: TextView
    private lateinit var timeRemainingLabel: TextView
    private lateinit var closeButton: Button

    // MARK: - Properties
    private var discount: AppUserDiscount? = null
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    // MARK: - Initialization
    init {
        setupUI()
    }

    // MARK: - UI Setup
    private fun setupUI() {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_discount_banner, this, true)

        // Initialize views
        containerView = findViewById(R.id.containerView)
        titleLabel = findViewById(R.id.titleLabel)
        descriptionLabel = findViewById(R.id.descriptionLabel)
        timeRemainingLabel = findViewById(R.id.timeRemainingLabel)
        closeButton = findViewById(R.id.closeButton)

        // Setup close button
        closeButton.setOnClickListener {
            hideDiscount()
        }

        // Set initial visibility
        visibility = View.GONE
    }

    // MARK: - Public Methods
    fun showDiscount(discount: AppUserDiscount) {
        println("🎯 DiscountBannerView.showDiscount called")
        println("  discount: $discount")
        
        this.discount = discount
        updateTimeRemaining()
        startTimer()

        // Set text
        titleLabel.text = "🎉 Special Discount!"
        descriptionLabel.text = "Limited time offer available for you!"

        // Debug logging
        println("🎯 DiscountBannerView Debug:")
        println("  Title text: ${titleLabel.text}")
        println("  Description text: ${descriptionLabel.text}")
        println("  Time remaining text: ${timeRemainingLabel.text}")
        println("  Visibility before: ${visibility}")
        println("  Alpha before: $alpha")
        println("  Parent: $parent")
        println("  Layout params: $layoutParams")

        // Animate in
        visibility = View.VISIBLE
        alpha = 0f
        translationY = 50f

        println("  Visibility after setting VISIBLE: ${visibility}")
        println("  Alpha after setting 0f: $alpha")

        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                println("  Animation completed")
                println("  Final visibility: ${visibility}")
                println("  Final alpha: $alpha")
            }
            .start()
    }

    fun hideDiscount() {
        stopTimer()

        // Animate out
        animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(300)
            .withEndAction {
                visibility = View.GONE
                (parent as? android.view.ViewGroup)?.removeView(this)
            }
            .start()
    }

    // MARK: - Private Methods
    private fun updateTimeRemaining() {
        val discount = this.discount ?: return

        val now = Date()
        val endTime = discount.endedAt
        val timeRemaining = endTime.time - now.time

        if (timeRemaining > 0) {
            val hours = (timeRemaining / (1000 * 60 * 60)).toInt()
            val minutes = ((timeRemaining % (1000 * 60 * 60)) / (1000 * 60)).toInt()

            timeRemainingLabel.text = if (hours > 0) {
                "⏰ ${hours}h ${minutes}m remaining"
            } else {
                "⏰ ${minutes}m remaining"
            }
        } else {
            timeRemainingLabel.text = "⏰ Expired"
            hideDiscount()
        }
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    updateTimeRemaining()
                }
            }
        }, 60000, 60000) // Update every minute
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }
} 