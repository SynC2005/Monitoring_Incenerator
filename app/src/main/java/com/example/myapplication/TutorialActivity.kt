package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.databinding.ActivityTutorialBinding
import com.google.android.material.tabs.TabLayoutMediator

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    private val pages = listOf(
        TutorialPage(
            imageRes = R.drawable.ic_books, // ganti sesuai drawable Anda
            title = "Selamat datang",
            desc = "Aplikasi ini membantu monitoring incinerator dan pencatatan data secara terstruktur."
        ),
        TutorialPage(
            imageRes = R.drawable.ic_qr, // drawable QR Anda
            title = "Scan QR",
            desc = "Gunakan tombol QR untuk scan credential mesin, lalu aktif/nonaktifkan status mesin."
        ),
        TutorialPage(
            imageRes = R.drawable.ic_data, // drawable data Anda
            title = "Lihat Data",
            desc = "Masuk ke menu Data untuk melihat daftar mesin, histori, dan status terkini."
        ),
        TutorialPage(
            imageRes = R.drawable.baseline_person_24,
            title = "Profil",
            desc = "Di menu User Anda dapat melihat informasi akun dan melakukan perubahan profil."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = TutorialPagerAdapter(pages)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabDots, binding.viewPager) { _, _ -> }.attach()

        updateButtons(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })

        binding.btnSkip.setOnClickListener {
            finish()
        }

        binding.btnNext.setOnClickListener {
            val pos = binding.viewPager.currentItem
            if (pos < pages.lastIndex) {
                binding.viewPager.currentItem = pos + 1
            } else {
                finish()
            }
        }
    }

    private fun updateButtons(position: Int) {
        val isLast = position == pages.lastIndex
        binding.btnNext.text = if (isLast) "Selesai" else "Lanjut"
        binding.btnSkip.text = if (isLast) "Tutup" else "Lewati"
    }
}
