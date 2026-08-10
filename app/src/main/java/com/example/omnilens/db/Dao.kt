package com.example.omnilens.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao {
    @Insert
    suspend fun insertClip(clip: Data)
    @Query("SELECT * FROM clips ORDER BY timestamp DESC")
    fun getAllClips(): Flow<List<Data>>

    @androidx.room.Update
    suspend fun updateClip(clip: Data)

    @androidx.room.Delete
    suspend fun deleteClip(clip: Data)
}