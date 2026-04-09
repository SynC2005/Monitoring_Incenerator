package com.example.myapplication

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class DataActivity : AppCompatActivity() {

    private lateinit var btnRefresh: Button
    private lateinit var btnDownload: Button
    private lateinit var recyclerTable: RecyclerView

    // Firestore instance
    private val db = FirebaseFirestore.getInstance()

    // Hold data in memory for display
    private val displayList = mutableListOf<TableRowData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        btnRefresh = findViewById(R.id.btnRefreshCSV) // You can rename this ID in XML to btnRefreshData
        btnDownload = findViewById(R.id.btnDownloadCSV)
        recyclerTable = findViewById(R.id.recyclerTable)
        recyclerTable.layoutManager = LinearLayoutManager(this)

        // Load data immediately on start
        fetchDataFromFirestore()

        btnRefresh.setOnClickListener {
            fetchDataFromFirestore()
        }

        btnDownload.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Generate CSV from the *currently displayed* data
                downloadCSVFromMemory()
            } else {
                Toast.makeText(this, "Android version too low for this download method", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= 1. FETCH REAL DATA FROM FIRESTORE =================
    private fun fetchDataFromFirestore() {
        Toast.makeText(this, "Memuat data...", Toast.LENGTH_SHORT).show()

        // Assuming you store history in a collection.
        // NOTE: In your ExcelWorker, you pulled from "Data_Incenerator".
        // If "Data_Incenerator" only has the CURRENT status, you need a separate collection
        // like "ActivityLogs" to track history.
        // Below I assume you want to see the list of machines like in ExcelWorker.

        db.collection("Data_Incenerator")
            .get()
            .addOnSuccessListener { snapshot ->
                displayList.clear()

                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Data kosong", Toast.LENGTH_SHORT).show()
                    updateAdapter()
                    return@addOnSuccessListener
                }

                // Temporary list to handle async user fetching
                val tempList = mutableListOf<TableRowData>()
                var processedCount = 0
                val totalDocs = snapshot.documents.size

                for (doc in snapshot.documents) {
                    val namaMesin = doc.id
                    val statusBool = doc.getBoolean("Status") ?: false
                    val status = if (statusBool) "ON" else "OFF"
                    val userId = doc.getString("userId")

                    // Logic to get Date. If Firestore doesn't have a timestamp field,
                    // we might have to just show "Current" or fetch a log collection.
                    // For now, I will use Current Time as placeholder or a field 'lastUpdated' if you add it.
                    val date = getCurrentDateTime()

                    // Fetch Username based on ID
                    if (!userId.isNullOrEmpty()) {
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val username = userDoc.getString("name") ?: userId
                                tempList.add(TableRowData(date, namaMesin, status, username))

                                processedCount++
                                if (processedCount == totalDocs) {
                                    // All data ready
                                    displayList.addAll(tempList)
                                    updateAdapter()
                                }
                            }
                            .addOnFailureListener {
                                // If fail, just use ID
                                tempList.add(TableRowData(date, namaMesin, status, userId))
                                processedCount++
                                if (processedCount == totalDocs) {
                                    displayList.addAll(tempList)
                                    updateAdapter()
                                }
                            }
                    } else {
                        // No user ID
                        tempList.add(TableRowData(date, namaMesin, status, "-"))
                        processedCount++
                        if (processedCount == totalDocs) {
                            displayList.addAll(tempList)
                            updateAdapter()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAdapter() {
        // Sort by Machine Name or Date if needed
        displayList.sortBy { it.namaMesin }
        recyclerTable.adapter = TableAdapter(displayList)
        Toast.makeText(this, "Data diperbarui", Toast.LENGTH_SHORT).show()
    }

    // ================= 2. GENERATE CSV FROM DATA =================
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadCSVFromMemory() {
        if (displayList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk didownload", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "laporan_incinerator_${System.currentTimeMillis()}.csv"

        // Build CSV Content String
        val csvHeader = "Tanggal,Nama Mesin,Status,User\n"
        val csvBody = StringBuilder()

        for (row in displayList) {
            // Escape commas in names to prevent CSV breaking
            val cleanName = row.namaMesin.replace(",", " ")
            val cleanUser = row.user.replace(",", " ")

            csvBody.append("${row.tanggal},$cleanName,${row.status},$cleanUser\n")
        }

        val fullContent = csvHeader + csvBody.toString()

        // Save using MediaStore (Same as your logic, but writing string data)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { out ->
                    out.write(fullContent.toByteArray())
                }
                Toast.makeText(this, "CSV tersimpan di Download", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal menulis file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}