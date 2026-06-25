package com.example.data.repository

import com.example.data.database.FaceDao
import com.example.data.database.FaceScanEntity
import com.example.data.database.FaceComparisonEntity
import kotlinx.coroutines.flow.Flow

class FaceRepository(private val faceDao: FaceDao) {
    val allScans: Flow<List<FaceScanEntity>> = faceDao.getAllScans()
    val allComparisons: Flow<List<FaceComparisonEntity>> = faceDao.getAllComparisons()

    suspend fun insertScan(scan: FaceScanEntity) {
        faceDao.insertScan(scan)
    }

    suspend fun deleteScanById(id: Int) {
        faceDao.deleteScanById(id)
    }

    suspend fun clearAllScans() {
        faceDao.clearAllScans()
    }

    suspend fun insertComparison(comparison: FaceComparisonEntity) {
        faceDao.insertComparison(comparison)
    }

    suspend fun deleteComparisonById(id: Int) {
        faceDao.deleteComparisonById(id)
    }

    suspend fun clearAllComparisons() {
        faceDao.clearAllComparisons()
    }
}
