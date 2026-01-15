package com.example.omnilens.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.omnilens.R

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        //Start as Foreground Service (Required for Android 8+)
        startForegroundServiceNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,

            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 100
        windowManager.addView(floatingView, layoutParams)

        setupTouchListener()

    }


    private fun startForegroundServiceNotification() {
        val channelId = "OmniLensOverlayChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OmniLens Service",
                NotificationManager.IMPORTANCE_LOW // Low importance = no annoying sound
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("OmniLens is Active")
            .setContentText("Tap to manage overlay")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    private fun setupTouchListener() {
        val bubbleIcon = floatingView.findViewById<View>(R.id.bubble_icon)

        bubbleIcon.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Remember the initial position of the VIEW
                        initialX = layoutParams.x
                        initialY = layoutParams.y

                        // Remember the initial position of the FINGER
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Calculate the shift
                        val xDiff = (event.rawX - initialTouchX).toInt()
                        val yDiff = (event.rawY - initialTouchY).toInt()

                        // Update the view position
                        layoutParams.x = initialX + xDiff
                        layoutParams.y = initialY + yDiff

                        // Tell WindowManager to redraw the view immediately
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val xDiff = (event.rawX - initialTouchX).toInt()
                        val yDiff = (event.rawY - initialTouchY).toInt()

                        if (kotlin.math.abs(xDiff) < 10 && kotlin.math.abs(yDiff) < 10) {
                            // It was a click!
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}