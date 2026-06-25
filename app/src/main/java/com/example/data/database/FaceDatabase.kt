package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "face_scans")
data class FaceScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val celebrityName: String,
    val celebritySimilarity: Int, // e.g. 94 for 94%
    val age: Int,
    val gender: String,
    val emotion: String,
    val symmetry: Int, // e.g. 92%
    val analysis: String,
    val webMatches: String // Format: "Title|Url;Title|Url"
)

@Entity(tableName = "face_comparisons")
data class FaceComparisonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath1: String? = null,
    val imagePath2: String? = null,
    val similarityScore: Int, // e.g. 88 for 88%
    val matchStatus: String, // e.g. "Same Person", "Highly Likely Match", "No Match"
    val analysisReport: String
)

@Dao
interface FaceDao {
    @Query("SELECT * FROM face_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<FaceScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: FaceScanEntity)

    @Query("DELETE FROM face_scans WHERE id = :id")
    suspend fun deleteScanById(id: Int)

    @Query("DELETE FROM face_scans")
    suspend fun clearAllScans()

    @Query("SELECT * FROM face_comparisons ORDER BY timestamp DESC")
    fun getAllComparisons(): Flow<List<FaceComparisonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComparison(comparison: FaceComparisonEntity)

    @Query("DELETE FROM face_comparisons WHERE id = :id")
    suspend fun deleteComparisonById(id: Int)

    @Query("DELETE FROM face_comparisons")
    suspend fun clearAllComparisons()
}

@Database(entities = [FaceScanEntity::class, FaceComparisonEntity::class], version = 1, exportSchema = false)
abstract class FaceDatabase : RoomDatabase() {
    abstract fun faceDao(): FaceDao

    companion object {
        @Volatile
        private var INSTANCE: FaceDatabase? = null

        fun getDatabase(context: Context): FaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    "face_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
