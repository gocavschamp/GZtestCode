package com.example.firstapplication.ui.kids.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 幼儿教育模块 Room 实体
 */

/** 通用学习进度键值对（已认识最大数 / 最佳连胜 / 已解锁难度） */
@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey val key: String,
    val value: Int
)

/** 已学会的汉字 */
@Entity(tableName = "learned_char")
data class LearnedCharEntity(
    @PrimaryKey val hanzi: String,
    var pinyin: String = "",
    var learnedAt: Long = System.currentTimeMillis()
)

/** 加减法答题记录（一局） */
@Entity(tableName = "arithmetic_record")
data class ArithmeticRecordEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val level: Int,
    val total: Int,
    val correct: Int,
    val streak: Int,
    val bestStreak: Int,
    var timestamp: Long = System.currentTimeMillis()
)
