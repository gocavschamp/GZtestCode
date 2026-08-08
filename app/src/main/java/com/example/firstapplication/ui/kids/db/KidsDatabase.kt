package com.example.firstapplication.ui.kids.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// ==================== DAO ====================

@Dao
interface ProgressDao {

    @Query("SELECT value FROM study_progress WHERE `key` = :key")
    suspend fun getValue(key: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: StudyProgressEntity)
}

@Dao
interface LearnedCharDao {

    @Query("SELECT * FROM learned_char ORDER BY learnedAt ASC")
    suspend fun getAll(): List<LearnedCharEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: LearnedCharEntity)

    @Query("SELECT COUNT(*) FROM learned_char")
    suspend fun count(): Int
}

@Dao
interface ArithmeticRecordDao {

    @Insert
    suspend fun insert(record: ArithmeticRecordEntity)

    @Query("SELECT COUNT(*) FROM arithmetic_record")
    suspend fun recordCount(): Int

    @Query("SELECT * FROM arithmetic_record ORDER BY id DESC LIMIT 20")
    suspend fun recentRecords(): List<ArithmeticRecordEntity>
}

// ==================== Database ====================

@Database(
    entities = [
        StudyProgressEntity::class,
        LearnedCharEntity::class,
        ArithmeticRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KidsDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao
    abstract fun learnedCharDao(): LearnedCharDao
    abstract fun recordDao(): ArithmeticRecordDao

    companion object {
        @Volatile
        private var INSTANCE: KidsDatabase? = null

        fun get(context: Context): KidsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidsDatabase::class.java,
                    "kids_learning.db"
                ).build().also { INSTANCE = it }
            }
    }
}
