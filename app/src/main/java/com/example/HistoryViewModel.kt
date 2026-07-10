package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HistoryRepository

    val historyItems: StateFlow<List<HistoryItem>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
        historyItems = repository.historyFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun addHistoryEntry(url: String, title: String) {
        if (url.isBlank() || url == "about:blank" || url.startsWith("file://")) return
        viewModelScope.launch {
            repository.insert(
                HistoryItem(
                    url = url,
                    title = if (title.isBlank()) url else title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }
}
