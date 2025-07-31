package com.cognivaa.amsapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class MyCaptureActivity : CaptureActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var flashButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var instructionText: TextView
    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_capture)

        initializeViews()
        setupCustomUI()
        setupClickListeners()
    }

    private fun initializeViews() {
        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        flashButton = findViewById(R.id.flashButton)
        closeButton = findViewById(R.id.closeButton)
        instructionText = findViewById(R.id.instructionText)
    }

    private fun setupCustomUI() {
        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)

        // Initialize barcode view
        barcodeView.initializeFromIntent(intent)
        barcodeView.decodeContinuous { result ->
            // Handle scan result
            setResult(RESULT_OK, intent.putExtra("SCAN_RESULT", result.text))
            finish()
        }

        // Update instruction text with animation
        instructionText.alpha = 0f
        instructionText.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()
    }

    private fun setupClickListeners() {
        flashButton.setOnClickListener {
            toggleFlash()
        }

        closeButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun toggleFlash() {
        if (isFlashOn) {
            barcodeView.setTorchOff()
            flashButton.setImageResource(R.drawable.ic_flash_off)
            isFlashOn = false
        } else {
            barcodeView.setTorchOn()
            flashButton.setImageResource(R.drawable.ic_flash_on)
            isFlashOn = true
        }

        // Add button animation
        flashButton.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                flashButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}