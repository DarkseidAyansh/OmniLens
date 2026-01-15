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

    private lateinit var tvStatusOverlay: TextView
    private lateinit var tvStatusAccess: TextView
    private lateinit var btnAction: Button

    // Launcher for Overlay Permission (Modern Way)
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Logic handled in onResume, but we keep this hook just in case
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

        tvStatusOverlay = findViewById(R.id.tv_status_overlay)
        tvStatusAccess = findViewById(R.id.tv_status_access)
        btnAction = findViewById(R.id.btn_action)

        updateUI()

        btnAction.setOnClickListener {
            handleButtonClick()
        }
    }

    /**
     * SENIOR ENGINEER TIP:
     * We use onResume() to auto-refresh the UI when the user returns from Settings.
     * This makes the app feel "smart" and responsive.
     */
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val isOverlayGranted = Settings.canDrawOverlays(this)
        val isAccessGranted = isAccessibilityEnabled()

        if (isOverlayGranted) {
            tvStatusOverlay.text = "Granted ✅"
            tvStatusOverlay.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvStatusOverlay.text = "Missing ❌"
            tvStatusOverlay.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        if (isAccessGranted) {
            tvStatusAccess.text = "Active ✅"
            tvStatusAccess.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvStatusAccess.text = "Inactive ❌"
            tvStatusAccess.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        when {
            !isOverlayGranted -> {
                btnAction.text = "Step 1: Grant Overlay Permission"
                btnAction.isEnabled = true
            }
            !isAccessGranted -> {
                btnAction.text = "Step 2: Enable Accessibility"
                btnAction.isEnabled = true
            }
            else -> {
                btnAction.text = "🚀 Start OmniLens"
                btnAction.isEnabled = true
            }
        }
    }

    private fun handleButtonClick() {
        when {
            !Settings.canDrawOverlays(this) -> requestOverlayPermission()
            !isAccessibilityEnabled() -> requestAccessibilityPermission()
            else -> startOverlayService()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        Toast.makeText(this, "Find 'OmniLens' and turn it ON", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        // Android 8.0+ Requirement
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        moveTaskToBack(true)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expectedService = "$packageName/${packageName}.service.OmniAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(expectedService)
    }
}