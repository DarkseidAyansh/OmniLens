package com.example.omnilens.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class OverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "Overlay Service Created!", Toast.LENGTH_LONG).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // If the system kills us, restart us automatically
    }
}