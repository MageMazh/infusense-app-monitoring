package com.d121211069.infusense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.d121211069.infusense.util.InfusStatus

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user: String,
    val dateTime: String,
    val location: String,
    val description: String,
    val status: InfusStatus
)