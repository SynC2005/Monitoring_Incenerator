package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.databinding.ActivityAddMachineBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.security.SecureRandom

class AddMachineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMachineBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // karakter yang boleh digunakan untuk credential
    private val CREDENTIAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val secureRandom = SecureRandom()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMachineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tambah Mesin"

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnGetCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.btnSaveMachine.setOnClickListener {
            saveMachine()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    binding.etLatitude.setText(location.latitude.toString())
                    binding.etLongitude.setText(location.longitude.toString())
                    Toast.makeText(this, "Lokasi berhasil didapatkan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Tidak bisa mendapatkan lokasi. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveMachine() {
        val machineName = binding.etMachineName.text.toString().trim()
        val latitudeStr = binding.etLatitude.text.toString().trim()
        val longitudeStr = binding.etLongitude.text.toString().trim()

        // Validasi input
        if (machineName.isEmpty()) {
            binding.etMachineName.error = "Nama mesin harus diisi"
            return
        }

        if (latitudeStr.isEmpty()) {
            binding.etLatitude.error = "Latitude harus diisi"
            return
        }

        if (longitudeStr.isEmpty()) {
            binding.etLongitude.error = "Longitude harus diisi"
            return
        }

        val latitude = latitudeStr.toDoubleOrNull()
        val longitude = longitudeStr.toDoubleOrNull()

        if (latitude == null || longitude == null) {
            Toast.makeText(this, "Koordinat tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi range koordinat
        if (latitude < -90 || latitude > 90) {
            binding.etLatitude.error = "Latitude harus antara -90 dan 90"
            return
        }

        if (longitude < -180 || longitude > 180) {
            binding.etLongitude.error = "Longitude harus antara -180 dan 180"
            return
        }

        // Generate credential otomatis (tidak tampil di UI)
        val credential = generateCredential(20)

        // Generate mapLink dari koordinat
        val mapLink = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        // Simpan ke Firestore
        val machineData = hashMapOf(
            "Credential" to credential,
            "Status" to false,
            "Latitude" to latitude,
            "Longitude" to longitude,
            "mapLink" to mapLink
        )

        db.collection("Data Incenerator")
            .document(machineName)
            .set(machineData)
            .addOnSuccessListener {
                Toast.makeText(this, "Mesin \"$machineName\" berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menambahkan mesin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permission ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // -----------------------
    // Fungsi helper: generate credential acak
    // -----------------------
    private fun generateCredential(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length) {
            val idx = secureRandom.nextInt(CREDENTIAL_CHARS.length)
            sb.append(CREDENTIAL_CHARS[idx])
        }
        return sb.toString()
    }
}
