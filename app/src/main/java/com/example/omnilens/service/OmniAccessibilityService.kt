package com.example.omnilens.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.omnilens.db.AppDatabase
import com.example.omnilens.db.ClipRepository
import com.example.omnilens.db.Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OmniAccessibilityService : AccessibilityService() {

    companion object {
        var instance: OmniAccessibilityService? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: ClipRepository

    var currentPackageName: String = "Unknown App"
    private var lastHighlightedText: String = ""
    private var lastSourceApp: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val dao = AppDatabase.getDatabase(applicationContext).clipDao()
        repository = ClipRepository(dao)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.packageName != null) {
            currentPackageName = event.packageName.toString()
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val node = event.source ?: return
            val start = node.textSelectionStart
            val end = node.textSelectionEnd

            if (start >= 0 && end > start) {
                val fullText = node.text?.toString()
                if (fullText != null) {
                    try {
                        lastHighlightedText = fullText.substring(start, end)
                        lastSourceApp = currentPackageName
                        Log.d("OmniLens", "Remembering highlight: $lastHighlightedText")
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e("OmniLens", "Highlight extraction failed", e)
                    }
                }
            }
            node.recycle()
        }
    }

    fun saveCurrentHighlight() {
        if (lastHighlightedText.isNotBlank()) {
            val textToSave = lastHighlightedText
            val appToSave = lastSourceApp

            lastHighlightedText = ""

            val clip = Data(text = textToSave, sourceApp = appToSave)

            serviceScope.launch {
                try {
                    repository.insert(clip)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Saved!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("OmniLens", "Database insert failed", e)
                }
            }
        } else {
            Toast.makeText(this, "Please highlight text first!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
}