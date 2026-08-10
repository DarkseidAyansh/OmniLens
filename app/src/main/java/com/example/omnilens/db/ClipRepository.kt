package com.example.omnilens.db

import kotlinx.coroutines.flow.Flow


class ClipRepository(private val clipDao: Dao) {

    val allClips: Flow<List<Data>> = clipDao.getAllClips()

    suspend fun insert(clip: Data) {
        clipDao.insertClip(clip)
    }

    suspend fun update(clip: Data) {
        clipDao.updateClip(clip)
    }

    suspend fun delete(clip: Data) {
        clipDao.deleteClip(clip)
    }
}