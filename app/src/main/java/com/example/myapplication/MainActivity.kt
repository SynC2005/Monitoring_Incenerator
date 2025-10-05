package com.example.myapplication

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var colorActive: Int = 0
    private var colorInactive: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize colors
        colorActive = ContextCompat.getColor(this, R.color.nav_active)
        colorInactive = ContextCompat.getColor(this, R.color.nav_inactive)

        // Set click listeners
        setupClickListeners()

        // Set Home as default selected
        selectNavItem(0)
    }

    private fun setupClickListeners() {
        binding.apply {
            navHome.setOnClickListener { selectNavItem(0) }
            navTimeline.setOnClickListener { selectNavItem(1) }
            navNotification.setOnClickListener { selectNavItem(2) }
            navProfile.setOnClickListener { selectNavItem(3) }

            fabAdd.setOnClickListener {
                // Handle QR scan button click
                // TODO: Open QR scanner activity or fragment
            }

            // Card click listeners
            cardDataDisplay.setOnClickListener {
                // TODO: Handle data display card click
            }

            cardMonitoring.setOnClickListener {
                // TODO: Handle monitoring card click
            }

            cardAdditionalInfo.setOnClickListener {
                // TODO: Handle additional info card click
            }
        }
    }

    private fun selectNavItem(position: Int) {
        // Reset all items to inactive state
        resetAllNavItems()

        binding.apply {
            when (position) {
                0 -> { // Home
                    iconHome.setColorFilter(colorActive)
                    labelHome.setTextColor(colorActive)
                    labelHome.setTypeface(null, Typeface.BOLD)
                    indicatorHome.visibility = View.VISIBLE
                    // TODO: Show home content (already visible in this layout)
                }
                1 -> { // Data/Timeline
                    iconTimeline.setColorFilter(colorActive)
                    labelTimeline.setTextColor(colorActive)
                    labelTimeline.setTypeface(null, Typeface.BOLD)
                    // TODO: Load Data/Timeline Fragment
                }
                2 -> { // Notification
                    iconNotification.setColorFilter(colorActive)
                    labelNotification.setTextColor(colorActive)
                    labelNotification.setTypeface(null, Typeface.BOLD)
                    // TODO: Load Notification Fragment
                }
                3 -> { // Profile
                    iconProfile.setColorFilter(colorActive)
                    labelProfile.setTextColor(colorActive)
                    labelProfile.setTypeface(null, Typeface.BOLD)
                    // TODO: Load Profile Fragment
                }
            }
        }
    }

    private fun resetAllNavItems() {
        binding.apply {
            // Reset Home
            iconHome.setColorFilter(colorInactive)
            labelHome.setTextColor(colorInactive)
            labelHome.setTypeface(null, Typeface.NORMAL)
            indicatorHome.visibility = View.GONE

            // Reset Timeline/Data
            iconTimeline.setColorFilter(colorInactive)
            labelTimeline.setTextColor(colorInactive)
            labelTimeline.setTypeface(null, Typeface.NORMAL)

            // Reset Notification
            iconNotification.setColorFilter(colorInactive)
            labelNotification.setTextColor(colorInactive)
            labelNotification.setTypeface(null, Typeface.NORMAL)

            // Reset Profile
            iconProfile.setColorFilter(colorInactive)
            labelProfile.setTextColor(colorInactive)
            labelProfile.setTypeface(null, Typeface.NORMAL)
        }
    }
}
