package com.example.omnilens.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.omnilens.R
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var panelView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        setupBubble()
        setupPanel()
    }

    private fun setupBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)

        bubbleParams = createLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = 0
        bubbleParams.y = 100

        setupBubbleTouch()
        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun setupPanel() {
        panelView = LayoutInflater.from(this).inflate(R.layout.service_control_panel, null)

        panelParams = createLayoutParams(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        panelParams.gravity = Gravity.CENTER

        panelView.findViewById<ImageButton>(R.id.btn_close_panel).setOnClickListener {
            closePanel()
        }

        panelView.findViewById<Button>(R.id.btn_read_text).setOnClickListener {
            closePanel()

            val service = OmniAccessibilityService.instance
            if (service != null) {
                service.saveCurrentHighlight()
            } else {
                Toast.makeText(this, "Accessibility Service is not active", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPanel() {
        try {
            windowManager.removeView(bubbleView)
            windowManager.addView(panelView, panelParams)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun closePanel() {
        try {
            windowManager.removeView(panelView)
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun createLayoutParams(flags: Int): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun setupBubbleTouch() {
        val bubbleIcon = bubbleView.findViewById<View>(R.id.bubble_icon)
        bubbleIcon.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val xDiff = abs(event.rawX - initialTouchX)
                        val yDiff = abs(event.rawY - initialTouchY)
                        if (xDiff < 10 && yDiff < 10) {
                            openPanel()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun startForegroundServiceNotification() {
        val channelId = "OmniLensOverlayChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "OmniLens Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("OmniLens is Active")
            .setContentText("Tap the bubble to open tools")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized) try { windowManager.removeView(bubbleView) } catch(_:Exception){}
        if (::panelView.isInitialized) try { windowManager.removeView(panelView) } catch(_:Exception){}
    }
}