package com.example.omnilens.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OmniAccessibilityService : AccessibilityService() {

    companion object {
        var instance: OmniAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("OmniLens", "Accessibility Service Connected")

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || rootInActiveWindow == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            Log.d("OmniLens", "Event from: ${event.packageName}")
        }
    }

    

    override fun onInterrupt() {
        Log.e("OmniLens", "Accessibility Service Interrupted")
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}