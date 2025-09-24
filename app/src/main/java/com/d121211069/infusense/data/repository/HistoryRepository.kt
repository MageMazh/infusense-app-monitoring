package com.d121211069.infusense.data.repository

import com.d121211069.infusense.data.local.entity.HistoryEntity
import com.d121211069.infusense.data.local.room.HistoryDao
import kotlinx.coroutines.flow.Flow

class HistoryRepository private constructor(
    private val historyDao: HistoryDao
) {

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun insertHistory(item: HistoryEntity) = historyDao.insertHistory(item)

    suspend fun deleteAllHistory() = historyDao.clearAll()

    companion object {
        @Volatile
        private var instance: HistoryRepository? = null
        fun getInstance(
            historyDao: HistoryDao
        ): HistoryRepository =
            instance ?: synchronized(this) {
                instance ?: HistoryRepository(historyDao)
            }.also { instance = it }
    }
}
