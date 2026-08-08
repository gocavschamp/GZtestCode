package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.SharedPreferences
import com.example.firstapplication.ui.kids.db.ArithmeticRecordEntity
import com.example.firstapplication.ui.kids.db.KidsDatabase
import com.example.firstapplication.ui.kids.db.LearnedCharEntity
import com.example.firstapplication.ui.kids.db.StudyProgressEntity
import kotlinx.coroutines.runBlocking

/**
 * 幼儿教育模块本地数据（第二版：Room 数据库存储）
 * 保存内容：数字认识完成进度、加减法最佳成绩/最高难度/答题记录、汉字学习进度
 * 对外保持同步 API（内部 runBlocking 包装轻量查询，数据量小，毫秒级）
 */
object KidsProgressStore {

    private fun db(context: Context): KidsDatabase = KidsDatabase.get(context)

    // ==================== 数字认识 ====================

    /** 已认识到的最大数字 */
    fun getMaxNumberLearned(context: Context): Int =
        runBlocking { db(context).progressDao().getValue(KEY_MAX_NUMBER) ?: 0 }

    fun setMaxNumberLearned(context: Context, number: Int) {
        runBlocking { db(context).progressDao().put(StudyProgressEntity(KEY_MAX_NUMBER, number)) }
    }

    /** 认识数字当前浏览到的位置（下次进入从上一次的数字继续） */
    fun getLastNumberLearned(context: Context): Int =
        runBlocking { db(context).progressDao().getValue(KEY_LAST_NUMBER) ?: 0 }

    fun setLastNumberLearned(context: Context, number: Int) {
        runBlocking { db(context).progressDao().put(StudyProgressEntity(KEY_LAST_NUMBER, number)) }
    }

    // ==================== 加减法 ====================

    /** 加减法最佳连续答对数 */
    fun getBestStreak(context: Context): Int =
        runBlocking { db(context).progressDao().getValue(KEY_BEST_STREAK) ?: 0 }

    /** 已解锁的最高难度（1/2/3） */
    fun getUnlockedLevel(context: Context): Int =
        runBlocking { db(context).progressDao().getValue(KEY_UNLOCKED_LEVEL) ?: 1 }

    fun setUnlockedLevel(context: Context, level: Int) {
        runBlocking { db(context).progressDao().put(StudyProgressEntity(KEY_UNLOCKED_LEVEL, level)) }
    }

    /** 记录一轮答题结果，返回是否刷新最佳纪录 */
    fun recordArithmeticResult(context: Context, streak: Int): Boolean {
        val best = getBestStreak(context)
        if (streak > best) {
            runBlocking { db(context).progressDao().put(StudyProgressEntity(KEY_BEST_STREAK, streak)) }
            return true
        }
        return false
    }

    /** 记录一局答题明细（首页统计答题次数用） */
    fun addArithmeticRecord(context: Context, level: Int, total: Int, correct: Int, streak: Int) {
        val best = getBestStreak(context)
        runBlocking {
            db(context).recordDao().insert(
                ArithmeticRecordEntity(
                    level = level,
                    total = total,
                    correct = correct,
                    streak = streak,
                    bestStreak = maxOf(best, streak)
                )
            )
        }
    }

    /** 累计完成答题局数 */
    fun getArithmeticRecordCount(context: Context): Int =
        runBlocking { db(context).recordDao().recordCount() }

    // ==================== 汉字 ====================

    /** 已学会的汉字列表 */
    fun getLearnedChars(context: Context): List<String> =
        runBlocking { db(context).learnedCharDao().getAll().map { it.hanzi } }

    /** 已学会汉字数量 */
    fun getLearnedCharCount(context: Context): Int =
        runBlocking { db(context).learnedCharDao().count() }

    fun markCharLearned(context: Context, char: String, pinyin: String = "") {
        runBlocking { db(context).learnedCharDao().insert(LearnedCharEntity(hanzi = char, pinyin = pinyin)) }
    }

    // ==================== 英语每日打卡 ====================

    private const val KEY_EN_CHECKIN_MONTH = "english_checkin_month"
    private const val KEY_EN_CHECKIN_MASK = "english_checkin_mask"

    /** 当月英语打卡掩码（第 n 天打卡 → 第 n-1 位为 1；跨月自动归零） */
    fun getEnglishCheckinMask(context: Context): Int = runBlocking {
        val month = db(context).progressDao().getValue(KEY_EN_CHECKIN_MONTH) ?: 0
        if (month != englishMonth()) 0
        else db(context).progressDao().getValue(KEY_EN_CHECKIN_MASK) ?: 0
    }

    /** 标记某天打卡完成，返回新掩码 */
    fun markEnglishCheckin(context: Context, day: Int): Int {
        val mask = getEnglishCheckinMask(context) or (1 shl (day - 1))
        runBlocking {
            db(context).progressDao().put(StudyProgressEntity(KEY_EN_CHECKIN_MONTH, englishMonth()))
            db(context).progressDao().put(StudyProgressEntity(KEY_EN_CHECKIN_MASK, mask))
        }
        return mask
    }

    private fun englishMonth(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.YEAR) * 100 + (cal.get(java.util.Calendar.MONTH) + 1)
    }

    // ==================== 英语互动游戏（100 关闯关进度） ====================

    private const val KEY_EN_GAME_PROGRESS = "english_game_progress"
    private const val KEY_EN_GAME_SCORE = "english_game_score"

    /** 已通关题数（0..100） */
    fun getEnglishGameProgress(context: Context): Int = runBlocking {
        db(context).progressDao().getValue(KEY_EN_GAME_PROGRESS) ?: 0
    }

    fun setEnglishGameProgress(context: Context, value: Int) {
        runBlocking {
            db(context).progressDao().put(StudyProgressEntity(KEY_EN_GAME_PROGRESS, value))
        }
    }

    /** 游戏累计得分 */
    fun getEnglishGameScore(context: Context): Int = runBlocking {
        db(context).progressDao().getValue(KEY_EN_GAME_SCORE) ?: 0
    }

    fun setEnglishGameScore(context: Context, value: Int) {
        runBlocking {
            db(context).progressDao().put(StudyProgressEntity(KEY_EN_GAME_SCORE, value))
        }
    }

    // ==================== 第一版 SharedPreferences 数据迁移 ====================

    /** 首次运行时把旧版 SharedPreferences 里的进度迁移进数据库（幂等） */
    fun migrateFromPrefs(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_MAX_NUMBER) && !prefs.contains(KEY_BEST_STREAK)) return
        runBlocking {
            val dao = db(context).progressDao()
            val learnedDao = db(context).learnedCharDao()
            prefs.getInt(KEY_MAX_NUMBER, 0).takeIf { it > 0 }
                ?.let { dao.put(StudyProgressEntity(KEY_MAX_NUMBER, it)) }
            prefs.getInt(KEY_BEST_STREAK, 0).takeIf { it > 0 }
                ?.let { dao.put(StudyProgressEntity(KEY_BEST_STREAK, it)) }
            prefs.getInt(KEY_UNLOCKED_LEVEL, 1).takeIf { it > 1 }
                ?.let { dao.put(StudyProgressEntity(KEY_UNLOCKED_LEVEL, it)) }
            prefs.getStringSet(KEY_LEARNED_CHARS, emptySet())
                ?.forEach { learnedDao.insert(LearnedCharEntity(hanzi = it)) }
            prefs.edit().clear().apply()
        }
    }

    private const val PREFS_NAME = "kids_learning_progress"
    private const val KEY_MAX_NUMBER = "max_number_learned"
    private const val KEY_LAST_NUMBER = "last_number_learned"
    private const val KEY_BEST_STREAK = "best_streak"
    private const val KEY_UNLOCKED_LEVEL = "unlocked_level"
    private const val KEY_LEARNED_CHARS = "learned_chars"
}
