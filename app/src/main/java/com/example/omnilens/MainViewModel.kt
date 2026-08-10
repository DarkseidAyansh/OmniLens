package com.example.omnilens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnilens.db.AppDatabase
import com.example.omnilens.db.ClipRepository
import com.example.omnilens.db.Data
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClipRepository
    val allClips: StateFlow<List<Data>>

    init {
        val clipDao = AppDatabase.getDatabase(application).clipDao()
        repository = ClipRepository(clipDao)

        allClips = repository.allClips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun saveClip(text: String, sourceApp: String) {
        viewModelScope.launch {
            val clip = Data(text = text, sourceApp = sourceApp)
            repository.insert(clip)
        }
    }

    fun updateClip(clip: Data) {
        viewModelScope.launch {
            repository.update(clip)
        }
    }

    fun deleteClip(clip: Data) {
        viewModelScope.launch {
            repository.delete(clip)
        }
    }

    fun insertClip(clip: Data) {
        viewModelScope.launch {
            repository.insert(clip)
        }
    }
}