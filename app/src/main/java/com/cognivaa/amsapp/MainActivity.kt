package com.cognivaa.amsapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cognivaa.amsapp.model.AttendanceRequest
import com.cognivaa.amsapp.network.RetrofitClient
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.cognivaa.amsapp.model.AttendanceResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var scanBtn: Button
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "AMS_PREFS"
        private const val LAST_SCAN_TIME = "last_scan_time"
        private const val SCAN_COOLDOWN_MS = 3000L // 3 seconds cooldown
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        hideLoading()

        if (result.contents != null) {
            val employeeCode = result.contents.trim()

            // Validate QR code format
            if (isValidEmployeeCode(employeeCode)) {
                // Check scan cooldown
                if (canScanNow()) {
                    processAttendance(employeeCode)
                } else {
                    showErrorDialog("Please wait", "Please wait a few seconds before scanning again.")
                }
            } else {
                showErrorDialog("Invalid QR Code", "The scanned QR code is not a valid employee code.")
            }
        } else {
            showToast("Scan cancelled", Toast.LENGTH_SHORT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ThreeTenABP
        AndroidThreeTen.init(this)

        // Setup window flags for immersive experience
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        initializePreferences()

        // Replace deprecated onBackPressed() with OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (loadingOverlay.visibility == View.VISIBLE) {
                    hideLoading()
                } else {
                    // Allow default back action
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun initializeViews() {
        scanBtn = findViewById(R.id.scanBtn)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    private fun setupClickListeners() {
        scanBtn.setOnClickListener {
            startQRScanning()
        }
    }

    private fun initializePreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun startQRScanning() {
        showLoading()

        Handler(Looper.getMainLooper()).postDelayed({
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Position QR code within the frame")
                setBeepEnabled(true)
                setBarcodeImageEnabled(true)
                setOrientationLocked(true)
                setCameraId(1)
                captureActivity = MyCaptureActivity::class.java
            }
            barcodeLauncher.launch(options)
        }, 300)
    }

    private fun processAttendance(employeeCode: String) {
        showLoading()

        val currentTime = LocalDateTime.now()
        val formattedDateTime = currentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val request = AttendanceRequest(employeeCode, formattedDateTime)

        RetrofitClient.instance.getAttendance(request)
            .enqueue(object : Callback<AttendanceResponse> {
                override fun onResponse(call: Call<AttendanceResponse>, response: Response<AttendanceResponse>) {
                    hideLoading()

                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data != null) {
                            updateLastScanTime()
                            showSuccessDialog(data)
                        } else {
                            showErrorDialog("Error", "No data received from server")
                        }
                    } else {
                        handleApiError(response.code())
                    }
                }

                override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                    hideLoading()
                    Log.e(TAG, "API Error: ", t)

                    val errorMessage = when {
                        t.message?.contains("Unable to resolve host") == true ->
                            "No internet connection. Please check your network."
                        t.message?.contains("timeout") == true ->
                            "Request timeout. Please try again."
                        else -> "Connection failed: ${t.message}"
                    }

                    showErrorDialog("Connection Error", errorMessage)
                }
            })
    }

    private fun showSuccessDialog(data: AttendanceResponse) {
        val formattedTime = formatDisplayTime(data.dateTime)

        AlertDialog.Builder(this)
            .setTitle("✅ Attendance Recorded")
            .setMessage("""
                Employee: ${data.employeeName}
                Time: $formattedTime
                Status: Successfully recorded
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Scan Another") { dialog, _ ->
                dialog.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({
                    startQRScanning()
                }, 500)
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ $title")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Try Again") { dialog, _ ->
                dialog.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({
                    startQRScanning()
                }, 500)
            }
            .show()
    }

    private fun handleApiError(errorCode: Int) {
        val errorMessage = when (errorCode) {
            400 -> "Invalid employee code"
            401 -> "Authentication failed"
            403 -> "Access denied"
            404 -> "Employee not found"
            500 -> "Server error. Please try again later"
            503 -> "Service temporarily unavailable"
            else -> "Error occurred (Code: $errorCode)"
        }

        showErrorDialog("Server Error", errorMessage)
    }

    private fun isValidEmployeeCode(code: String): Boolean {
        return code.isNotEmpty() && code.length >= 3 && code.matches(Regex("^[A-Za-z0-9]+$"))
    }

    private fun canScanNow(): Boolean {
        val lastScanTime = sharedPreferences.getLong(LAST_SCAN_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastScanTime) > SCAN_COOLDOWN_MS
    }

    private fun updateLastScanTime() {
        sharedPreferences.edit()
            .putLong(LAST_SCAN_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun formatDisplayTime(dateTime: String): String {
        return try {
            val localDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
        } catch (e: Exception) {
            dateTime
        }
    }

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
        scanBtn.isEnabled = false
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
        scanBtn.isEnabled = true
    }

    private fun showToast(message: String, duration: Int) {
        Toast.makeText(this, message, duration).show()
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
    }
}
