package com.example.myapplication

import android.content.Intent
import android.graphics.Color
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
    private var machineListener: ListenerRegistration? = null // 🔄 listener realtime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavbar()
        setupCardListeners()
        setupAddMachineButton()

        // 🔥 Jalankan realtime listener
        listenMachinesFromFirestore()
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
        cardMonitoring.setOnClickListener {
            Toast.makeText(this@MainActivity, "Monitoring clicked", Toast.LENGTH_SHORT).show()
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

                if (result == null || result.isEmpty) {
                    val emptyText = TextView(this).apply {
                        text = "Belum ada data mesin."
                        setTextColor(Color.GRAY)
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setPadding(0, 32, 0, 32)
                    }
                    container.addView(emptyText)
                    return@addSnapshotListener
                }

                // 🔄 Render semua mesin
                for (doc in result) {
                    val machineName = doc.id
                    val status = doc.getBoolean("Status") ?: false

                    // 🔹 Gunakan CardView agar tumpul dan ada bayangan
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

                    // 🔹 Layout isi card (horizontal)
                    val innerLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(20, 24, 20, 24)
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    // 🔹 Nama mesin
                    val nameView = TextView(this).apply {
                        text = machineName
                        textSize = 16f
                        setTextColor(Color.BLACK)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    // 🔹 Status label (tumpul dan berwarna)
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

                    // 🔹 Tombol hapus (ikon)
                    val deleteButton = android.widget.ImageView(this).apply {
                        setImageResource(R.drawable.ic_trashbin)
                        setColorFilter(Color.parseColor("#F44336"))
                        setPadding(24, 12, 0, 12)
                        setOnClickListener {
                            // 🟡 Pop-up konfirmasi sebelum menghapus
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
