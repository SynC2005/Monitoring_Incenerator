package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.NavbarBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navbarHelper: NavbarHelper
    private val db = FirebaseFirestore.getInstance()
    private var machineListener: ListenerRegistration? = null
    private val machines = mutableListOf<Machine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavbar()
        setupCardListeners()
        setupAddMachineButton()
        setupMapButton()

        listenMachinesFromFirestore()
    }

    private fun setupMapButton() {
        binding.tvOpenMap.setOnClickListener {
            openFullMap()
        }

        binding.cardMonitoring.setOnClickListener {
            openFullMap()
        }
    }

    private fun openFullMap() {
        // Cek apakah Google Maps app tersedia
        if (MapsUtils.isGooglePlayServicesAvailable(this)) {
            // Buka MapsActivity jika Play Services tersedia
            try {
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback ke Google Maps app/web
                openGoogleMapsExternal()
            }
        } else {
            // Fallback ke Google Maps app/web
            openGoogleMapsExternal()
        }
    }

    private fun openGoogleMapsExternal() {
        if (machines.isEmpty()) {
            Toast.makeText(this, "Belum ada data mesin", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil mesin pertama sebagai center atau rata-rata dari semua mesin
        val validMachines = machines.filter { it.latitude != 0.0 && it.longitude != 0.0 }

        if (validMachines.isEmpty()) {
            Toast.makeText(this, "Tidak ada mesin dengan lokasi valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat URL untuk multiple markers
        val markers = validMachines.joinToString("|") {
            "${it.latitude},${it.longitude}"
        }

        val firstMachine = validMachines.first()
        val mapUrl = "https://www.google.com/maps/search/?api=1&query=${firstMachine.latitude},${firstMachine.longitude}"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMachineLocationsList() {
        val container = binding.machineLocationsList
        container.removeAllViews()

        if (machines.isEmpty()) {
            val statusText = TextView(this).apply {
                text = "Belum ada data mesin"
                setTextColor(Color.GRAY)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            container.addView(statusText)
            return
        }

        val validMachines = machines.filter { it.latitude != 0.0 && it.longitude != 0.0 }

        if (validMachines.isEmpty()) {
            val statusText = TextView(this).apply {
                text = "Tidak ada mesin dengan lokasi valid"
                setTextColor(Color.GRAY)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            container.addView(statusText)
            return
        }

        // Header
        val headerText = TextView(this).apply {
            text = "${validMachines.size} Mesin Terdaftar"
            setTextColor(Color.parseColor("#333333"))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        container.addView(headerText)

        // List mesin dengan koordinat
        validMachines.take(5).forEach { machine ->
            val machineRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                gravity = Gravity.CENTER_VERTICAL
            }

            // Status indicator
            val statusDot = android.view.View(this).apply {
                val params = LinearLayout.LayoutParams(12, 12)
                params.setMargins(0, 0, 12, 0)
                layoutParams = params
                setBackgroundResource(
                    if (machine.status) android.R.drawable.presence_online
                    else android.R.drawable.presence_busy
                )
            }

            // Machine info
            val machineInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameText = TextView(this).apply {
                text = machine.name
                setTextColor(Color.parseColor("#333333"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val coordText = TextView(this).apply {
                text = "📍 ${String.format("%.4f", machine.latitude)}, ${String.format("%.4f", machine.longitude)}"
                setTextColor(Color.parseColor("#666666"))
                textSize = 11f
            }

            machineInfo.addView(nameText)
            machineInfo.addView(coordText)

            // Link button
            val linkButton = TextView(this).apply {
                text = "Lihat"
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 12f
                setPadding(12, 4, 12, 4)
                setOnClickListener {
                    openMachineLocation(machine)
                }
            }

            machineRow.addView(statusDot)
            machineRow.addView(machineInfo)
            machineRow.addView(linkButton)

            container.addView(machineRow)

            // Divider
            if (machine != validMachines.last()) {
                val divider = android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
                container.addView(divider)
            }
        }

        // Show more button jika ada lebih dari 5 mesin
        if (validMachines.size > 5) {
            val showMoreButton = TextView(this).apply {
                text = "Lihat ${validMachines.size - 5} mesin lainnya"
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 0)
                setOnClickListener {
                    openFullMap()
                }
            }
            container.addView(showMoreButton)
        }
    }

    private fun openMachineLocation(machine: Machine) {
        if (machine.mapLink.isNotEmpty()) {
            // Gunakan mapLink yang sudah ada
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(machine.mapLink))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak dapat membuka link", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Buat link dari koordinat
            val mapUrl = "https://www.google.com/maps/search/?api=1&query=${machine.latitude},${machine.longitude}"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak dapat membuka Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavbar() {
        val navbarBinding: NavbarBinding = binding.bottomNavigation
        navbarHelper = NavbarHelper(
            binding = navbarBinding,
            onNavigationItemSelected = { position -> handleNavigation(position) },
            onFabClicked = { handleQRScan() }
        )
        navbarHelper.selectNavItem(0)
    }

    private fun setupAddMachineButton() {
        binding.tvAddMachine.setOnClickListener {
            val intent = Intent(this, AddMachineActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCardListeners() = with(binding) {
        cardDataDisplay.setOnClickListener {
            Toast.makeText(this@MainActivity, "Data Display clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenMachinesFromFirestore() {
        val container = binding.machineContainer
        machineListener?.remove()

        machineListener = db.collection("Data Incenerator")
            .addSnapshotListener { result, error ->
                if (error != null) {
                    Toast.makeText(this, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                container.removeAllViews()
                machines.clear()

                if (result == null || result.isEmpty) {
                    val emptyText = TextView(this).apply {
                        text = "Belum ada data mesin."
                        setTextColor(Color.GRAY)
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setPadding(0, 32, 0, 32)
                    }
                    container.addView(emptyText)
                    updateMachineLocationsList()
                    return@addSnapshotListener
                }

                for (doc in result) {
                    val machineName = doc.id
                    val status = doc.getBoolean("Status") ?: false
                    val credential = doc.getString("Credential") ?: ""
                    val mapLink = doc.getString("mapLink") ?: ""

                    // Prioritas: Baca dari field Latitude/Longitude langsung
                    var latitude = doc.getDouble("Latitude") ?: 0.0
                    var longitude = doc.getDouble("Longitude") ?: 0.0

                    // Fallback: Extract dari mapLink jika field tidak ada
                    if (latitude == 0.0 && longitude == 0.0 && mapLink.isNotEmpty()) {
                        val coordinates = Machine.extractCoordinatesFromMapLink(mapLink)
                        latitude = coordinates?.first ?: 0.0
                        longitude = coordinates?.second ?: 0.0
                    }

                    machines.add(Machine(machineName, status, latitude, longitude, credential, mapLink))

                    val card = androidx.cardview.widget.CardView(this).apply {
                        radius = 20f
                        cardElevation = 6f
                        setCardBackgroundColor(Color.WHITE)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 20)
                        layoutParams = params
                    }

                    val innerLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(20, 24, 20, 24)
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val nameView = TextView(this).apply {
                        text = machineName
                        textSize = 16f
                        setTextColor(Color.BLACK)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val statusView = TextView(this).apply {
                        text = if (status) "Aktif" else "Non Aktif"
                        textSize = 14f
                        setTextColor(Color.WHITE)
                        setPadding(28, 12, 28, 12)
                        background = resources.getDrawable(
                            if (status) R.drawable.bg_status_active else R.drawable.bg_status_inactive,
                            theme
                        )
                    }

                    val deleteButton = android.widget.ImageView(this).apply {
                        setImageResource(R.drawable.ic_trashbin)
                        setColorFilter(Color.parseColor("#F44336"))
                        setPadding(24, 12, 0, 12)
                        setOnClickListener {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Konfirmasi Hapus")
                                .setMessage("Apakah Anda yakin ingin menghapus mesin \"$machineName\"?")
                                .setCancelable(true)
                                .setPositiveButton("Ya") { dialog, _ ->
                                    db.collection("Data Incenerator").document(machineName)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Mesin \"$machineName\" berhasil dihapus",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Gagal menghapus mesin: ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Tidak") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }

                    innerLayout.addView(nameView)
                    innerLayout.addView(statusView)
                    innerLayout.addView(deleteButton)

                    card.addView(innerLayout)
                    container.addView(card)
                }

                updateMachineLocationsList()
            }
    }

    override fun onResume() {
        super.onResume()
        if (machineListener == null) {
            listenMachinesFromFirestore()
        }
    }

    override fun onPause() {
        super.onPause()
        machineListener?.remove()
        machineListener = null
    }

    private fun handleNavigation(position: Int) {
        // tetap sama
    }

    private fun handleQRScan() {
        Toast.makeText(this, "Opening QR Scanner...", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, QRScannerActivity::class.java))
    }
}