package com.example.firstapplication.ui.kids

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.firstapplication.databinding.ActivityKidsHomeBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.random.Random

/**
 * 幼儿教育首页（App 主入口）：数字乐园 / 加减法 / 汉字乐园 / H5 小游戏 + 演示中心
 * 动画：太阳旋转、云朵漂移、星星飘落、气球上升、卡片入场滑入、点击回弹、进度条平滑滚动
 */
@Route(path = "/module/kids/home")
class KidsHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKidsHomeBinding
    private var isActive = true

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKidsHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)

        // 数据迁移（第一版 SharedPreferences → Room，幂等）与语音初始化
        KidsProgressStore.migrateFromPrefs(this)
        KidsTts.init(this)

        setupListeners()

        // 布局测量完成后播放入场与循环动画
        binding.root.post {
            playEntranceAnimations()
            playLoopAnimations()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从子页面返回时刷新进度（进度条平滑滚动）
        refreshProgress()
    }

    private fun refreshProgress() {
        // 数字认识进度（上限 1000，对应 0-999）
        val maxNumber = KidsProgressStore.getMaxNumberLearned(this)
        animateProgress(binding.progressNumber, maxNumber, 1000)
        binding.tvNumberProgress.text = String.format("认识数字: %d / 1000 个", maxNumber)

        // 加减法进度
        val bestStreak = KidsProgressStore.getBestStreak(this)
        val recordCount = KidsProgressStore.getArithmeticRecordCount(this)
        animateProgress(binding.progressArithmetic, bestStreak, 10)
        binding.tvArithmeticProgress.text = String.format("最佳连胜: %d · 已玩 %d 局", bestStreak, recordCount)

        // 汉字进度
        val learned = KidsProgressStore.getLearnedCharCount(this)
        val total = ChineseWordActivity.CHAR_TOTAL
        animateProgress(binding.progressChinese, learned, total)
        binding.tvChineseProgress.text = String.format("已学汉字: %d / %d 个", learned, total)
    }

    /** 进度条从 0 平滑滚动到目标值 */
    private fun animateProgress(bar: LinearProgressIndicator, target: Int, max: Int) {
        bar.max = max
        ValueAnimator.ofInt(0, target.coerceIn(0, max)).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener { bar.progress = it.animatedValue as Int }
            start()
        }
    }

    private fun setupListeners() {
        bindCardClick(binding.cardNumber) { NumberLearningActivity.start(this) }
        bindCardClick(binding.cardArithmetic) { NumberArithmeticActivity.start(this) }
        bindCardClick(binding.cardChinese) { ChineseWordActivity.start(this) }
        bindCardClick(binding.cardH5) { KidsH5Activity.start(this) }
        bindCardClick(binding.cardPoetry) { PoetryActivity.start(this) }
        bindCardClick(binding.cardEnglish) { EnglishActivity.start(this) }
        bindCardClick(binding.cardDemo) {
            // 原 MainActivity 演示中心入口
            ARouter.getInstance().build("/module/main").navigation()
        }
    }

    /** 卡片点击回弹动画（按压缩小 → 弹起放大 → 恢复） */
    private fun bindCardClick(card: MaterialCardView, action: () -> Unit) {
        card.setOnClickListener {
            card.animate().scaleX(0.94f).scaleY(0.94f).setDuration(50).withEndAction {
                card.animate().scaleX(1.04f).scaleY(1.04f).setDuration(60).withEndAction {
                    card.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    action()
                }.start()
            }.start()
        }
    }

    // ==================== 入场动画 ====================

    private fun playEntranceAnimations() {
        // 标题弹跳入场
        binding.tvTitle.apply {
            alpha = 0f
            scaleX = 0.4f
            scaleY = 0.4f
        }
        binding.tvTitle.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(500).setStartDelay(80)
            .setInterpolator(OvershootInterpolator(2.2f)).start()

        binding.tvSubtitle.alpha = 0f
        binding.tvSubtitle.animate().alpha(1f).setDuration(400).setStartDelay(360).start()

        // 卡片从下方依次滑入
        val cards = listOf(
            binding.cardNumber, binding.cardArithmetic,
            binding.cardChinese, binding.cardH5, binding.cardPoetry,
            binding.cardEnglish, binding.cardDemo
        )
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 110f
            card.animate().alpha(1f).translationY(0f)
                .setDuration(520).setStartDelay(320L + index * 120L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    // ==================== 循环动画 ====================

    private fun playLoopAnimations() {
        animateCloud(binding.tvCloud1, 46f, 5200L)
        animateCloud(binding.tvCloud2, 34f, 6800L)
        animateSun()
        repeat(6) { spawnStar() }
        repeat(3) { spawnBalloon() }
    }

    /** 云朵左右来回漂移 */
    private fun animateCloud(view: View, dist: Float, duration: Long) {
        if (!isActive) return
        view.animate().translationX(dist).setDuration(duration).setInterpolator(LinearInterpolator())
            .withEndAction {
                view.animate().translationX(0f).setDuration(duration).setInterpolator(LinearInterpolator())
                    .withEndAction { animateCloud(view, dist, duration) }.start()
            }.start()
    }

    /** 太阳缓慢旋转（带轻微呼吸缩放） */
    private fun animateSun() {
        if (!isActive) return
        binding.tvSun.animate().rotation(360f).setDuration(20000).setInterpolator(LinearInterpolator())
            .withEndAction {
                binding.tvSun.rotation = 0f
                animateSun()
            }.start()
        binding.tvSun.animate().scaleX(1.15f).scaleY(1.15f).setDuration(1200).setStartDelay(0)
            .withEndAction {
                binding.tvSun.animate().scaleX(1f).scaleY(1f).setDuration(1200).start()
            }.start()
    }

    /** 星星从顶部落下，落下后重新生成（保持约 6 颗） */
    private fun spawnStar() {
        if (!isActive) return
        val root = binding.contentRoot
        val w = root.width
        val h = root.height
        if (w <= 0 || h <= 0) return
        val star = TextView(this).apply {
            text = if (Random.nextBoolean()) "✨" else "⭐"
            textSize = 12f + Random.nextInt(4) * 4f
            x = Random.nextInt(w).toFloat()
            y = -40f
            alpha = 0f
        }
        root.addView(star)
        val duration = 3000L + Random.nextLong(2200)
        star.animate().y((h + 50).toFloat()).alpha(0.85f).setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction { root.removeView(star) }
            .start()
        star.postDelayed({ spawnStar() }, 700L + Random.nextLong(900))
    }

    /** 气球/飞机从底部升起，升起后重新生成 */
    private fun spawnBalloon() {
        if (!isActive) return
        val root = binding.contentRoot
        val w = root.width
        val h = root.height
        if (w <= 0 || h <= 0) return
        val isPlane = Random.nextBoolean()
        val balloon = TextView(this).apply {
            text = if (isPlane) "✈️" else "🎈"
            textSize = 20f + Random.nextInt(16)
            x = Random.nextInt(w).toFloat()
            y = (h + 40).toFloat()
            alpha = 0f
            // 飞机随机倾斜，更生动
            if (isPlane) rotation = -20f + Random.nextInt(40).toFloat()
        }
        root.addView(balloon)
        val duration = 4000L + Random.nextLong(2200)
        balloon.animate().y(-60f).alpha(0.9f).setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction { root.removeView(balloon) }
            .start()
        balloon.postDelayed({ spawnBalloon() }, 1600L + Random.nextLong(1600))
    }
}
