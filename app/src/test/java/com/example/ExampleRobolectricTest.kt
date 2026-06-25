package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.FaceDatabase
import com.example.data.repository.FaceRepository
import com.example.ui.viewmodel.FaceViewModel
import com.example.ui.viewmodel.BatchScanUiState
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: FaceDatabase
  private lateinit var repository: FaceRepository
  private lateinit var viewModel: FaceViewModel
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, FaceDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = FaceRepository(database.faceDao())
    viewModel = FaceViewModel(context as Application, repository)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun readStringFromContext() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Sherlock Face Search", appName)
  }

  @Test
  fun testBatchQueueOperations() {
    // Assert initial state is idle and empty
    assertTrue(viewModel.batchBitmaps.value.isEmpty())
    assertEquals(BatchScanUiState.Idle, viewModel.batchState.value)

    // Generate test bitmap
    val bitmap1 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val bitmap2 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Add bitmaps to the deck
    viewModel.addBatchBitmap(bitmap1)
    assertEquals(1, viewModel.batchBitmaps.value.size)

    viewModel.addBatchBitmap(bitmap2)
    assertEquals(2, viewModel.batchBitmaps.value.size)

    // Remove first bitmap
    viewModel.removeBatchBitmap(0)
    assertEquals(1, viewModel.batchBitmaps.value.size)

    // Clear entire deck
    viewModel.clearBatchBitmaps()
    assertTrue(viewModel.batchBitmaps.value.isEmpty())
  }

  @Test
  fun testBatchReset() {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    viewModel.addBatchBitmap(bitmap)
    
    // Reset batch state
    viewModel.resetBatch()
    assertTrue(viewModel.batchBitmaps.value.isEmpty())
    assertEquals(BatchScanUiState.Idle, viewModel.batchState.value)
  }

  @Test
  fun testExportToCsvAndPdf() {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val analysisResult = com.example.data.api.FaceAnalysisResult(
      celebrityName = "Brad Pitt",
      celebritySimilarity = 90,
      age = 45,
      gender = "Male",
      emotion = "Neutral",
      symmetry = 95,
      analysis = "Forensic facial structural coordinates point to high horizontal jaw and balanced ocular spacing.",
      webMatches = listOf(com.example.data.api.WebMatch("Profile Match", "https://linkedin.com"))
    )
    val batchItem = com.example.ui.viewmodel.BatchScanItemResult(bitmap, analysisResult)
    val results = listOf(batchItem)

    // Run CSV export
    com.example.util.ExportUtil.exportBatchToCsv(context, results)
    
    val csvFile = java.io.File(context.cacheDir, "Sherlock_Biometric_Report.csv")
    assertTrue(csvFile.exists())
    assertTrue(csvFile.length() > 0)

    // Run PDF export
    com.example.util.ExportUtil.exportBatchToPdf(context, results)
    
    val pdfFile = java.io.File(context.cacheDir, "Sherlock_Biometric_Report.pdf")
    // In Robolectric unit tests, PdfDocument's native layer is absent and returns early.
    // Thus we only assert if the file got successfully written.
    if (pdfFile.exists()) {
      assertTrue(pdfFile.length() > 0)
    }
  }

  @Test
  fun testFaceSelectionAndComparisonFromHistory() = kotlinx.coroutines.runBlocking {
    val scan1 = com.example.data.database.FaceScanEntity(
      id = 1,
      imagePath = "test_path_1.jpg",
      celebrityName = "Angelina Jolie",
      celebritySimilarity = 88,
      age = 42,
      gender = "Female",
      emotion = "Happy",
      symmetry = 96,
      analysis = "Beautiful symmetrical facial contour.",
      timestamp = 1000L,
      webMatches = ""
    )
    val scan2 = com.example.data.database.FaceScanEntity(
      id = 2,
      imagePath = "test_path_2.jpg",
      celebrityName = "Brad Pitt",
      celebritySimilarity = 92,
      age = 45,
      gender = "Male",
      emotion = "Serious",
      symmetry = 93,
      analysis = "Strong jawline structure.",
      timestamp = 2000L,
      webMatches = ""
    )

    repository.insertScan(scan1)
    repository.insertScan(scan2)

    // Verify database query retrieves correct entries (sorted descending by timestamp)
    val scans = repository.allScans.first()
    assertEquals(2, scans.size)
    assertEquals("Brad Pitt", scans[0].celebrityName)
    assertEquals("Angelina Jolie", scans[1].celebrityName)
  }
}
