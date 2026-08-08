package com.example.firstapplication.ui.kids

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityKidsHomeBinding

/**
 * 幼儿教育首页（App 主入口）：数字乐园 / 加减法 / 汉字乐园 + 演示中心
 */
@Route(path = "/module/kids/home")
class KidsHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKidsHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKidsHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 数据迁移（第一版 SharedPreferences → Room，幂等）与语音初始化
        KidsProgressStore.migrateFromPrefs(this)
        KidsTts.init(this)

        refreshProgress()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // 从子页面返回时刷新进度
        refreshProgress()
    }

    private fun refreshProgress() {
        // 数字认识进度（默认上限 30）
        val maxNumber = KidsProgressStore.getMaxNumberLearned(this)
        binding.progressNumber.max = 30
        binding.progressNumber.progress = maxNumber
        binding.tvNumberProgress.text = String.format("认识数字: %d / 30 个", maxNumber)

        // 加减法进度
        val bestStreak = KidsProgressStore.getBestStreak(this)
        val recordCount = KidsProgressStore.getArithmeticRecordCount(this)
        binding.progressArithmetic.max = 10
        binding.progressArithmetic.progress = bestStreak.coerceAtMost(10)
        binding.tvArithmeticProgress.text = String.format("最佳连胜: %d · 已玩 %d 局", bestStreak, recordCount)

        // 汉字进度
        val learned = KidsProgressStore.getLearnedCharCount(this)
        val total = ChineseWordActivity.CHAR_TOTAL
        binding.progressChinese.max = total
        binding.progressChinese.progress = learned
        binding.tvChineseProgress.text = String.format("已学汉字: %d / %d 个", learned, total)
    }

    private fun setupListeners() {
        binding.cardNumber.setOnClickListener {
            NumberLearningActivity.start(this)
        }
        binding.cardArithmetic.setOnClickListener {
            NumberArithmeticActivity.start(this)
        }
        binding.cardChinese.setOnClickListener {
            ChineseWordActivity.start(this)
        }
        binding.cardDemo.setOnClickListener {
            // 原 MainActivity 演示中心入口
            ARouter.getInstance().build("/module/main").navigation()
        }
    }
}
