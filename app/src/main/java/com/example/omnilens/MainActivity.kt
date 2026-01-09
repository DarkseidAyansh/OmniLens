package com.example.omnilens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.omnilens.service.OverlayService

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var btnStartOverlay: Button

    // Modern way to handle activity results (replacing onActivityResult)
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOverlayPermission()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusText = findViewById(R.id.tv_status)
        btnStartOverlay = findViewById(R.id.btn_start_overlay)

        checkOverlayPermission()

        btnStartOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else {
                startOverlayService()
            }
        }

    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            statusText.text = "Status: Permission Granted ✅"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            btnStartOverlay.text = "Start System Overlay"
        } else {
            statusText.text = "Status: Permission Missing ❌"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            btnStartOverlay.text = "Grant Overlay Permission"
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startOverlayService() {
        Toast.makeText(this, "Starting Overlay Service...", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, OverlayService::class.java)
        startService(intent)

    }
}