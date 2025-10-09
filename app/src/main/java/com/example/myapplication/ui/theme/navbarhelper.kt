package com.example.myapplication

import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.NavbarBinding

class NavbarHelper(
    private val binding: NavbarBinding,
    private val onNavigationItemSelected: (Int) -> Unit = {},
    private val onFabClicked: () -> Unit = {}
) {

    // 🎨 Colors
    private val colorActive: Int
    private val colorInactive: Int

    init {
        val context = binding.root.context
        colorActive = ContextCompat.getColor(context, R.color.pastel_green)   // active color
        colorInactive = ContextCompat.getColor(context, R.color.gray_999)    // inactive color
        setupClickListeners()
    }

    // 🔘 Setup all click listeners
    private fun setupClickListeners() = with(binding) {
        navHome.setOnClickListener { selectNavItem(0); onNavigationItemSelected(0) }
        navTimeline.setOnClickListener { selectNavItem(1); onNavigationItemSelected(1) }
        navNotification.setOnClickListener { selectNavItem(2); onNavigationItemSelected(2) }
        navProfile.setOnClickListener { selectNavItem(3); onNavigationItemSelected(3) }
        fabAdd.setOnClickListener { onFabClicked() }
    }

    // 🌟 Highlight selected item
    fun selectNavItem(position: Int) {
        resetAllNavItems()

        when (position) {
            0 -> {
                binding.iconHome.setColorFilter(colorActive)
                binding.labelHome.setTextColor(colorActive)
                binding.labelHome.setTypeface(null, Typeface.BOLD)
            }
            1 -> {
                binding.iconTimeline.setColorFilter(colorActive)
                binding.labelTimeline.setTextColor(colorActive)
                binding.labelTimeline.setTypeface(null, Typeface.BOLD)
            }
            2 -> {
                binding.iconNotification.setColorFilter(colorActive)
                binding.labelNotification.setTextColor(colorActive)
                binding.labelNotification.setTypeface(null, Typeface.BOLD)
            }
            3 -> {
                binding.iconProfile.setColorFilter(colorActive)
                binding.labelProfile.setTextColor(colorActive)
                binding.labelProfile.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    // ♻️ Reset all items to inactive state
    private fun resetAllNavItems() = with(binding) {
        listOf(
            iconHome, iconTimeline, iconNotification, iconProfile
        ).forEach { it.setColorFilter(colorInactive) }

        listOf(
            labelHome, labelTimeline, labelNotification, labelProfile
        ).forEach {
            it.setTextColor(colorInactive)
            it.setTypeface(null, Typeface.NORMAL)
        }
    }
}
