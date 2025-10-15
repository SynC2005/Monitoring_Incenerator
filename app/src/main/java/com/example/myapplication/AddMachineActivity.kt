package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class AddMachineActivity : AppCompatActivity() {

    private lateinit var etMachineName: EditText
    private lateinit var etLocationLink: EditText
    private lateinit var btnSave: Button
    private val db = FirebaseFirestore.getInstance()

    // simpan QR sementara
    private var tempBitmap: Bitmap? = null
    private var tempFileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_machine)

        etMachineName = findViewById(R.id.etMachineName)
        etLocationLink = findViewById(R.id.etLocationLink)
        btnSave = findViewById(R.id.btnSaveMachine)

        // Izin tulis (Android 9 ke bawah)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }

        btnSave.setOnClickListener { saveMachine() }
    }

    private fun saveMachine() {
        val name = etMachineName.text.toString().trim()
        val locationLink = etLocationLink.text.toString().trim()

        if (name.isEmpty() || locationLink.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = generateCredential(20)

        val data = mapOf(
            "Credential" to credential,
            "Status" to false,
            "mapLink" to locationLink
        )

        db.collection("Data Incenerator")
            .document(name)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                generateQRCodeAndAskToSave(credential, name)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan data ke database", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateCredential(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    // 🔹 Generate QR lalu minta user pilih lokasi penyimpanan
    private fun generateQRCodeAndAskToSave(content: String, machineName: String) {
        try {
            if (content.isEmpty()) {
                Toast.makeText(this, "Credential kosong, QR gagal dibuat", Toast.LENGTH_SHORT).show()
                return
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)

            val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            tempBitmap = bmp
            tempFileName = "QR_$machineName.png"

            // 🔹 Buka dialog sistem untuk pilih lokasi simpan
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/png"
                putExtra(Intent.EXTRA_TITLE, tempFileName)
            }
            saveFileLauncher.launch(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuat QR Code: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 🔹 Menangani hasil pemilihan lokasi simpan file
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null && tempBitmap != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    Toast.makeText(this, "✅ QR Code berhasil disimpan!", Toast.LENGTH_LONG).show()
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal menyimpan file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Gagal: QR kosong atau lokasi tidak dipilih", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Penyimpanan dibatalkan oleh pengguna", Toast.LENGTH_SHORT).show()
        }
    }
}
