package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.FaceAnalysisResult
import com.example.data.api.FaceComparisonResult
import com.example.data.api.GeminiClient
import com.example.data.database.FaceScanEntity
import com.example.data.database.FaceComparisonEntity
import com.example.data.repository.FaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class FaceViewModel(
    private val application: Application,
    private val repository: FaceRepository
) : AndroidViewModel(application) {

    // Tab Navigation
    private val _currentTab = MutableStateFlow(Tab.SCAN)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(tab: Tab) {
        _currentTab.value = tab
    }

    // --- Single Face Scan State ---
    private val _scanBitmap = MutableStateFlow<Bitmap?>(null)
    val scanBitmap = _scanBitmap.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState = _scanState.asStateFlow()

    fun setScanBitmap(bitmap: Bitmap?) {
        _scanBitmap.value = bitmap
        _scanState.value = ScanUiState.Idle
    }

    fun scanFace() {
        val bitmap = _scanBitmap.value ?: return
        _scanState.value = ScanUiState.Scanning

        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeFace(bitmap)
                
                // Save image to local cache directory to reference in Room
                val imagePath = saveBitmapToCache(bitmap)

                // Insert into Room DB
                val entity = FaceScanEntity(
                    imagePath = imagePath,
                    celebrityName = result.celebrityName,
                    celebritySimilarity = result.celebritySimilarity,
                    age = result.age,
                    gender = result.gender,
                    emotion = result.emotion,
                    symmetry = result.symmetry,
                    analysis = result.analysis,
                    webMatches = result.webMatches.joinToString(";") { "${it.title}|${it.url}" }
                )
                repository.insertScan(entity)

                _scanState.value = ScanUiState.Success(result)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Analysis failed")
            }
        }
    }

    fun resetScan() {
        _scanBitmap.value = null
        _scanState.value = ScanUiState.Idle
    }

    // --- Face Comparison State ---
    private val _compareBitmap1 = MutableStateFlow<Bitmap?>(null)
    val compareBitmap1 = _compareBitmap1.asStateFlow()

    private val _compareBitmap2 = MutableStateFlow<Bitmap?>(null)
    val compareBitmap2 = _compareBitmap2.asStateFlow()

    private val _compareState = MutableStateFlow<CompareUiState>(CompareUiState.Idle)
    val compareState = _compareState.asStateFlow()

    fun setCompareBitmap1(bitmap: Bitmap?) {
        _compareBitmap1.value = bitmap
        _compareState.value = CompareUiState.Idle
    }

    fun setCompareBitmap2(bitmap: Bitmap?) {
        _compareBitmap2.value = bitmap
        _compareState.value = CompareUiState.Idle
    }

    fun compareFaces() {
        val bmp1 = _compareBitmap1.value ?: return
        val bmp2 = _compareBitmap2.value ?: return
        _compareState.value = CompareUiState.Analyzing

        viewModelScope.launch {
            try {
                val result = GeminiClient.compareFaces(bmp1, bmp2)

                // Save both bitmaps to cache
                val path1 = saveBitmapToCache(bmp1)
                val path2 = saveBitmapToCache(bmp2)

                // Insert into Room DB
                val entity = FaceComparisonEntity(
                    imagePath1 = path1,
                    imagePath2 = path2,
                    similarityScore = result.similarityScore,
                    matchStatus = result.matchStatus,
                    analysisReport = result.analysisReport
                )
                repository.insertComparison(entity)

                _compareState.value = CompareUiState.Success(result)
            } catch (e: Exception) {
                _compareState.value = CompareUiState.Error(e.message ?: "Comparison failed")
            }
        }
    }

    fun resetComparison() {
        _compareBitmap1.value = null
        _compareBitmap2.value = null
        _compareState.value = CompareUiState.Idle
    }

    // --- History Lists from Room ---
    val scanHistory: StateFlow<List<FaceScanEntity>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val comparisonHistory: StateFlow<List<FaceComparisonEntity>> = repository.allComparisons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteScan(id: Int) {
        viewModelScope.launch {
            repository.deleteScanById(id)
        }
    }

    fun clearAllScans() {
        viewModelScope.launch {
            repository.clearAllScans()
        }
    }

    fun deleteComparison(id: Int) {
        viewModelScope.launch {
            repository.deleteComparisonById(id)
        }
    }

    fun clearAllComparisons() {
        viewModelScope.launch {
            repository.clearAllComparisons()
        }
    }

    // --- Helper to convert URI or InputStream to Bitmap ---
    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = application.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val cacheDir = application.cacheDir
            val file = File(cacheDir, "face_${UUID.randomUUID()}.jpg")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

enum class Tab {
    SCAN, COMPARE, HISTORY
}

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Scanning : ScanUiState
    data class Success(val result: FaceAnalysisResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface CompareUiState {
    object Idle : CompareUiState
    object Analyzing : CompareUiState
    data class Success(val result: FaceComparisonResult) : CompareUiState
    data class Error(val message: String) : CompareUiState
}

class FaceViewModelFactory(
    private val application: Application,
    private val repository: FaceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FaceViewModel::class.java)) {
            return FaceViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
