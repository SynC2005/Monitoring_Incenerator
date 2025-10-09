package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityQrScannerBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var cameraExecutor: ExecutorService
    private var scannedMachineId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚡ Inisialisasi binding pertama kali
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi executor untuk kamera
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Tombol back
        binding.btnBackHome.setOnClickListener { finish() }

        // Tombol status
        binding.btnActivate.setOnClickListener { updateMachineStatus("active") }
        binding.btnDeactivate.setOnClickListener { updateMachineStatus("inactive") }

        // Tombol tambah mesin
        binding.btnAddMachine.setOnClickListener { showAddMachineDialog() }

        // Cek izin kamera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, QRAnalyzer { qrText ->
                    runOnUiThread {
                        binding.tvResult.text = "Kode Mesin: $qrText"
                        scannedMachineId = qrText
                    }
                }) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memulai kamera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateMachineStatus(status: String) {
        val machineId = scannedMachineId ?: run {
            Toast.makeText(this, "Scan mesin terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val data = mapOf("status" to status)
        db.collection("machines").document(machineId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Status mesin $machineId diubah menjadi $status",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddMachineDialog() {
        val input = EditText(this)
        input.hint = "Masukkan ID Mesin Baru (contoh: machine_03)"

        AlertDialog.Builder(this)
            .setTitle("Tambah Mesin Baru")
            .setView(input)
            .setPositiveButton("Tambah") { _, _ ->
                val newMachineId = input.text.toString().trim()
                if (newMachineId.isNotEmpty()) {
                    addNewMachineToFirebase(newMachineId)
                } else {
                    Toast.makeText(this, "ID mesin tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun addNewMachineToFirebase(machineId: String) {
        val data = mapOf(
            "name" to "Mesin Baru",
            "status" to "inactive"
        )

        db.collection("machines").document(machineId)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Mesin $machineId berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                generateQRCode(machineId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menambah mesin", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateQRCode(machineId: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(machineId, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            binding.imgGeneratedQR.setImageBitmap(bmp)
            binding.imgGeneratedQR.visibility = android.view.View.VISIBLE
            binding.tvResult.text = "QR Code untuk mesin $machineId berhasil dibuat"

        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuat QR Code", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // stop executor
    }
}

// Analyzer QR code
private class QRAnalyzer(private val onQRCodeFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let {
                        onQRCodeFound(it)
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener { it.printStackTrace() }
            .addOnCompleteListener { imageProxy.close() }
    }
}
