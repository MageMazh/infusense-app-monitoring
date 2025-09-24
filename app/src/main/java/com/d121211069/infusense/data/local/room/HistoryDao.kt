package com.d121211069.infusense.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.d121211069.infusense.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(historyItem: HistoryEntity)

    @Query("SELECT * FROM history")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
