package com.example

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val historyFlow: Flow<List<HistoryItem>> = historyDao.getHistoryFlow()

    suspend fun insert(item: HistoryItem) {
        historyDao.insertHistoryItem(item)
    }

    suspend fun deleteById(id: Long) {
        historyDao.deleteHistoryItemById(id)
    }

    suspend fun clear() {
        historyDao.clearHistory()
    }
}
