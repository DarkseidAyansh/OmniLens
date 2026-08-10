package com.example.omnilens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.omnilens.db.ClipAdapter
import com.example.omnilens.db.Data
import com.example.omnilens.service.OverlayService
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var layoutSetup: View
    private lateinit var layoutDashboard: View
    private lateinit var fabLaunchBubble: FloatingActionButton

    // Setup Elements
    private lateinit var tvStatusOverlay: TextView
    private lateinit var tvStatusAccess: TextView
    private lateinit var btnAction: Button

    // Dashboard Elements
    private lateinit var chipGroupFolders: ChipGroup
    private lateinit var emptyStateContainer: View
    private lateinit var recyclerView: RecyclerView

    private val viewModel: MainViewModel by viewModels()
    private var currentSelectedFolder = "All"
    private lateinit var adapter: ClipAdapter

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        layoutSetup = findViewById(R.id.layout_setup)
        layoutDashboard = findViewById(R.id.layout_dashboard)
        fabLaunchBubble = findViewById(R.id.fab_launch_bubble)

        tvStatusOverlay = findViewById(R.id.tv_status_overlay)
        tvStatusAccess = findViewById(R.id.tv_status_access)
        btnAction = findViewById(R.id.btn_action)

        chipGroupFolders = findViewById(R.id.chip_group_folders)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        recyclerView = findViewById(R.id.recycler_history)

        setupRecyclerView()

        btnAction.setOnClickListener { handleSetupButtonClick() }
        fabLaunchBubble.setOnClickListener { startOverlayService() }

        observeDatabase()

        updateUI()
    }

    private fun setupRecyclerView() {
        adapter = ClipAdapter(
            onShareClick = { clip -> shareClip(clip) },
            onEditClick = { clip -> showEditDialog(clip) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelperCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, tgt: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val clipToDelete = adapter.getClipAt(viewHolder.adapterPosition)
                viewModel.deleteClip(clipToDelete)

                com.google.android.material.snackbar.Snackbar.make(
                    findViewById(R.id.main), "Clip deleted", com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("Undo") {
                    viewModel.insertClip(clipToDelete)
                }.show()
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun observeDatabase() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allClips.collect { allClips ->
                    val folders = mutableListOf("All")
                    folders.addAll(allClips.map { it.sourceApp }.distinct().sorted())

                    chipGroupFolders.removeAllViews()
                    for (folder in folders) {
                        val chip = Chip(this@MainActivity).apply {
                            text = folder
                            isCheckable = true
                            isChecked = (folder == currentSelectedFolder)
                            setOnClickListener {
                                currentSelectedFolder = folder
                                updateListDisplay(allClips)
                            }
                        }
                        chipGroupFolders.addView(chip)
                    }
                    updateListDisplay(allClips)
                }
            }
        }
    }

    private fun updateListDisplay(allClips: List<Data>) {
        val filteredClips = if (currentSelectedFolder == "All") {
            allClips
        } else {
            allClips.filter { it.sourceApp == currentSelectedFolder }
        }
        adapter.setData(filteredClips)

        if (filteredClips.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val isOverlayGranted = Settings.canDrawOverlays(this)
        val isAccessGranted = isAccessibilityEnabled()

        if (isOverlayGranted && isAccessGranted) {
            layoutSetup.visibility = View.GONE
            layoutDashboard.visibility = View.VISIBLE
            fabLaunchBubble.visibility = View.VISIBLE
        } else {
            layoutSetup.visibility = View.VISIBLE
            layoutDashboard.visibility = View.GONE
            fabLaunchBubble.visibility = View.GONE

            if (isOverlayGranted) {
                tvStatusOverlay.text = "Granted"
                tvStatusOverlay.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                tvStatusOverlay.text = "Missing"
                tvStatusOverlay.setTextColor(getColor(android.R.color.holo_red_dark))
            }

            if (isAccessGranted) {
                tvStatusAccess.text = "Active"
                tvStatusAccess.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                tvStatusAccess.text = "Inactive"
                tvStatusAccess.setTextColor(getColor(android.R.color.holo_red_dark))
            }

            btnAction.text = if (!isOverlayGranted) "Step 1: Grant Overlay Permission" else "Step 2: Enable Accessibility"
        }
    }

    private fun handleSetupButtonClick() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else if (!isAccessibilityEnabled()) {
            requestAccessibilityPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        Toast.makeText(this, "Find 'OmniLens' and turn it ON", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Bubble Launched!", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true) // Minimize the app so they can use the bubble
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expectedService = "$packageName/${packageName}.service.OmniAccessibilityService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServices.contains(expectedService)
    }

    private fun shareClip(clip: Data) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Copied from ${clip.sourceApp}")
            putExtra(Intent.EXTRA_TEXT, clip.text)
        }
        startActivity(Intent.createChooser(intent, "Share via..."))
    }

    private fun showEditDialog(clip: Data) {
        val editText = android.widget.EditText(this).apply {
            setText(clip.text)
            setPadding(48, 48, 48, 48)
            background = null
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Clip")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                if (newText != clip.text) {
                    viewModel.updateClip(clip.copy(text = newText))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}