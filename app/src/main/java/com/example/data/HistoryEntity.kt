package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val kaspaAmount: String,
    val fiatAmount: String,
    val fiatCurrency: String,
    val timestamp: Long
)
