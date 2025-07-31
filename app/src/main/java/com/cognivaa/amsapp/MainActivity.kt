package com.cognivaa.amsapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val employeeCode = result.contents
            val dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val request = AttendanceRequest(employeeCode, dateTime)

            RetrofitClient.instance.getAttendance(request)
                .enqueue(object : Callback<AttendanceResponse> {
                    override fun onResponse(call: Call<AttendanceResponse>, response: Response<AttendanceResponse>) {
                        if (response.isSuccessful) {
                            val data = response.body()
                            Toast.makeText(
                                this@MainActivity,
                                "Name: ${data?.employeeName}\nTime: ${data?.dateTime}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                        Log.e("API_ERROR", "Error: ", t)
                        Toast.makeText(this@MainActivity, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ThreeTenABP
        AndroidThreeTen.init(this)
        setContentView(R.layout.activity_main)

        val scanBtn = findViewById<Button>(R.id.scanBtn)
        scanBtn.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan QR code")
                setBeepEnabled(true)
                setBarcodeImageEnabled(true)
                setOrientationLocked(true)
                setCameraId(1)
                captureActivity = MyCaptureActivity::class.java
            }
            barcodeLauncher.launch(options)
        }
    }
}