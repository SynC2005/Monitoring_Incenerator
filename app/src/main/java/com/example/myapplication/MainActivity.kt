package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.NavbarBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navbarHelper: NavbarHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavbar()
        setupCardListeners()
    }

    private fun setupNavbar() {
        // Ambil binding dari include layout navbar
        val navbarBinding: NavbarBinding = binding.bottomNavigation

        // Inisialisasi helper
        navbarHelper = NavbarHelper(
            binding = navbarBinding,
            onNavigationItemSelected = { position ->
                handleNavigation(position)
            },
            onFabClicked = {
                handleQRScan()
            }
        )

        // Set default tab: Home
        navbarHelper.selectNavItem(0)
    }

    private fun setupCardListeners() = with(binding) {
        cardDataDisplay.setOnClickListener {
            Toast.makeText(this@MainActivity, "Data Display clicked", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to DataActivity
        }

        cardMonitoring.setOnClickListener {
            Toast.makeText(this@MainActivity, "Monitoring clicked", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to MonitoringActivity
        }

        cardAdditionalInfo.setOnClickListener {
            Toast.makeText(this@MainActivity, "Additional Info clicked", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to InfoActivity
        }
    }

    private fun handleNavigation(position: Int) {
        when (position) {
            0 -> {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                // TODO: Show home fragment or stay on this screen
            }
            1 -> {
                Toast.makeText(this, "Data", Toast.LENGTH_SHORT).show()
                // TODO: startActivity(Intent(this, DataActivity::class.java))
            }
            2 -> {
                Toast.makeText(this, "Notification", Toast.LENGTH_SHORT).show()
                // TODO: startActivity(Intent(this, NotificationActivity::class.java))
            }
            3 -> {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                // TODO: startActivity(Intent(this, ProfileActivity::class.java))
            }
        }
    }

    private fun handleQRScan() {
        Toast.makeText(this, "Opening QR Scanner...", Toast.LENGTH_SHORT).show()
        // TODO: startActivity(Intent(this, QRScannerActivity::class.java))
    }
}

