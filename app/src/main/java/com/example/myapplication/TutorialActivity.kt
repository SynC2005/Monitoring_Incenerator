package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.databinding.ActivityTutorialBinding
import com.google.android.material.tabs.TabLayoutMediator

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    // --- BAGIAN INI YANG HILANG (Daftar Halaman) ---
    private val pages = listOf(
        TutorialPage(
            imageRes = R.drawable.ic_launcher_foreground, // Pastikan gambar ini ada atau ganti dengan gambar kamu
            title = "Selamat Datang",
            desc = "Aplikasi ini membantu monitoring incinerator dan pencatatan data secara terstruktur."
        ),
        TutorialPage(
            imageRes = R.drawable.ic_launcher_foreground, // Ganti dengan R.drawable.ic_qr jika ada
            title = "Scan QR",
            desc = "Gunakan tombol QR untuk scan credential mesin, lalu aktif/nonaktifkan status mesin."
        ),
        TutorialPage(
            imageRes = R.drawable.ic_launcher_foreground, // Ganti dengan R.drawable.ic_data jika ada
            title = "Lihat Data",
            desc = "Masuk ke menu Data untuk melihat daftar mesin, histori, dan status terkini."
        ),
        TutorialPage(
            imageRes = R.drawable.ic_launcher_foreground, // Ganti dengan R.drawable.baseline_person_24 jika ada
            title = "Profil",
            desc = "Di menu User Anda dapat melihat informasi akun dan melakukan perubahan profil."
        )
    )
    // ------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hapus tombol back di toolbar (opsional)
        binding.toolbar.navigationIcon = null

        // Setup Adapter dengan data 'pages'
        val adapter = TutorialPagerAdapter(pages)
        binding.viewPager.adapter = adapter

        // Hubungkan titik-titik indikator (Dots)
        TabLayoutMediator(binding.tabDots, binding.viewPager) { _, _ -> }.attach()

        // Update tombol awal
        updateButtons(0)

        // Listener saat geser halaman
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })

        // Tombol Skip/Tutup
        binding.btnSkip.setOnClickListener {
            closeTutorial()
        }

        // Tombol Next/Selesai
        binding.btnNext.setOnClickListener {
            val pos = binding.viewPager.currentItem
            if (pos < pages.lastIndex) {
                binding.viewPager.currentItem = pos + 1
            } else {
                closeTutorial()
            }
        }
    }

    private fun closeTutorial() {
        // Simpan status bahwa user sudah melihat tutorial (jika perlu)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("hasSeenOnboarding", true).apply()

        // Kembali ke MainActivity atau tutup activity ini
        // Jika tutorial diakses dari dalam aplikasi (navbar), cukup finish()
        if (isTaskRoot) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun updateButtons(position: Int) {
        val isLast = position == pages.lastIndex
        binding.btnNext.text = if (isLast) "Selesai" else "Lanjut"
        binding.btnSkip.text = if (isLast) "Tutup" else "Lewati"
    }
}