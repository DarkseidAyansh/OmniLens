package com.example.omnilens.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clips")
data class Data(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sourceApp: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)