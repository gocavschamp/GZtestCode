package com.example.firstapplication.ui.kids

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityNumberLearningBinding
import com.example.firstapplication.ui.kids.NumberLearningActivity.Companion.GAME_COUNT
import com.example.firstapplication.ui.kids.NumberLearningActivity.Companion.GAME_FIND
import kotlin.random.Random

/**
 * 数字乐园：认识数字（0-999，随机数字模块）+ 找数字（听音选数）+ 数一数（多彩图标点数）
 * 难度：3-5岁(0-200) / 5-7岁(0-500) / 7-9岁(0-999) / 🎲 随机(0-999)
 */
class NumberLearningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberLearningBinding

    private var currentLevel = 1
    private var maxNumber = 200
    private var currentNumber = 0
    private var isRandomMode = false

    // 游戏状态
    private var gameMode = GAME_FIND
    private var gameScore = 0
    private var gameAnswer = 0

    private val colors = intArrayOf(
        0xFFFFB74D.toInt(), 0xFFFF8A80.toInt(), 0xFFAED581.toInt(),
        0xFF80DEEA.toInt(), 0xFFD1C4E9.toInt(), 0xFFF48FB1.toInt(),
        0xFFFFD54F.toInt(), 0xFF81C784.toInt(), 0xFF4FC3F7.toInt(),
        0xFFBA68C8.toInt(), 0xFFA1887F.toInt()
    )

    private val praises = arrayOf("真棒！", "太厉害了！", "好聪明呀！", "太棒了！", "你真棒！")

    /** 数一数用的多彩图标（系统 emoji） */
    data class CountIcon(val name: String, val emoji: String)

    private val countIcons = listOf(
        CountIcon("苹果", "🍎"), CountIcon("小车", "🚗"), CountIcon("电视", "📺"),
        CountIcon("钻石", "💎"), CountIcon("小兔", "🐰"), CountIcon("小猫", "🐱"),
        CountIcon("小狗", "🐶"), CountIcon("熊猫", "🐼"), CountIcon("蝴蝶", "🦋"),
        CountIcon("小鱼", "🐟"), CountIcon("小鸡", "🐥"), CountIcon("小鸭", "🐤"),
        CountIcon("草莓", "🍓"), CountIcon("气球", "🎈"), CountIcon("火箭", "🚀"),
        CountIcon("足球", "⚽")
    )

    /** 数字中文名 0-999（百位含"零"，如 205 = 二百零五） */
    private val numberNames: Array<String> by lazy {
        val digits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        Array(1000) { n ->
            when {
                n < 10 -> digits[n]
                n < 100 -> {
                    val t = n / 10
                    val u = n % 10
                    val tensName = if (t == 1) "十" else digits[t] + "十"
                    if (u == 0) tensName else tensName + digits[u]
                }
                else -> {
                    val h = n / 100
                    val rest = n % 100
                    val base = digits[h] + "百"
                    when {
                        rest == 0 -> base
                        rest < 10 -> base + "零" + digits[rest]
                        rest % 10 == 0 -> base + if (rest / 10 == 1) "十" else digits[rest / 10] + "十"
                        else -> base + (if (rest / 10 == 1) "十" else digits[rest / 10] + "十") + digits[rest % 10]
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumberLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)

        setupLevelChips()
        setupNavListeners()
        setupModeSwitch()
        setupGameListeners()
        setupReset()
        updateDisplay()
    }

    // ==================== 认识数字 ====================

    private fun setupLevelChips() {
        binding.chipLevel1.isChecked = true
        binding.chipLevel1.setOnClickListener { switchLevel(1, 200, random = false) }
        binding.chipLevel2.setOnClickListener { switchLevel(2, 500, random = false) }
        binding.chipLevel3.setOnClickListener { switchLevel(3, 999, random = false) }
        // 🎲 随机数字：每次切换 / 下一个 都随机 0-999
        binding.chipLevel4.setOnClickListener { switchLevel(4, 999, random = true) }
    }

    private fun switchLevel(level: Int, max: Int, random: Boolean) {
        currentLevel = level
        maxNumber = max
        isRandomMode = random
        currentNumber = if (random) {
            Random.nextInt(0, max + 1)
        } else {
            // 每个级别从上一个级别结束的数字开始（本地记录位置，跨级别衔接、下次进入恢复）
            KidsProgressStore.getLastNumberLearned(this).coerceIn(0, max)
        }
        updateDisplay()
    }

    private fun setupNavListeners() {
        binding.btnPrev.setOnClickListener {
            if (isRandomMode) {
                currentNumber = Random.nextInt(0, maxNumber + 1)
                updateDisplay()
            } else {
                goToNumber(currentNumber - 1)
            }
        }
        binding.btnNext.setOnClickListener {
            if (isRandomMode) {
                currentNumber = Random.nextInt(0, maxNumber + 1)
                updateDisplay()
            } else {
                goToNumber(currentNumber + 1)
            }
        }
        // 点击数字卡片：随机模式换随机数，普通模式进入下一个数字
        binding.cardNumber.setOnClickListener {
            if (isRandomMode) {
                currentNumber = Random.nextInt(0, maxNumber + 1)
                updateDisplay()
            } else {
                goToNumber(currentNumber + 1)
            }
        }
        // 点击喇叭：朗读当前数字
        binding.btnSpeak.setOnClickListener {
            KidsTts.speak("${numberNames[currentNumber]}，$currentNumber")
        }
    }

    /** 跳转到指定数字并本地记录位置（下次从这里继续） */
    private fun goToNumber(target: Int) {
        currentNumber = target.coerceIn(0, maxNumber)
        KidsProgressStore.setLastNumberLearned(this, currentNumber)
        val maxLearned = KidsProgressStore.getMaxNumberLearned(this)
        if (currentNumber > maxLearned) {
            KidsProgressStore.setMaxNumberLearned(this, currentNumber)
        }
        updateDisplay()
    }

    /** 重置按键：确认后从 0 重新开始 */
    private fun setupReset() {
        binding.btnResetNumber.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("↺ 重置学习进度")
                .setMessage("将从 0 重新开始认识数字，确定吗？")
                .setPositiveButton("确定") { _, _ ->
                    KidsProgressStore.setLastNumberLearned(this, 0)
                    KidsProgressStore.setMaxNumberLearned(this, 0)
                    currentNumber = 0
                    updateDisplay()
                    KidsTts.speak("重新开始，从零开始")
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun updateDisplay() {
        binding.tvNumber.text = currentNumber.toString()
        binding.tvNumber.setTextColor(colors[currentNumber % colors.size])
        // 数字变大时自动缩小字号
        binding.tvNumber.textSize = when {
            currentNumber >= 100 -> 92f
            currentNumber >= 10 -> 120f
            else -> 150f
        }
        binding.tvCountName.text = numberNames[currentNumber]
        binding.tvProgress.text = if (isRandomMode) {
            "🎲 随机数字 (0-$maxNumber)"
        } else {
            "第 ${currentNumber + 1} / ${maxNumber + 1} 个"
        }
        // 学习位置提示（普通模式记录进度，随机模式不记录）
        binding.tvLearnedInfo.text = if (isRandomMode) {
            "🎲 随机模式不记录进度"
        } else {
            "✨ 已学到 $currentNumber，下次从这里继续"
        }
        // 切换数字时语音朗读（本地 TTS）
        KidsTts.speak("${numberNames[currentNumber]}，$currentNumber")
        binding.tvNumber.animate()
            .scaleX(1.18f).scaleY(1.18f).setDuration(130)
            .withEndAction {
                binding.tvNumber.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
        // 数量圆点
        binding.dotsContainer.removeAllViews()
        if (currentNumber <= 20) {
            repeat(currentNumber) { index ->
                val dot = TextView(this)
                dot.text = "●"
                dot.textSize = 12f
                dot.setTextColor(colors[index % colors.size])
                binding.dotsContainer.addView(dot)
            }
        } else {
            val dot = TextView(this)
            dot.text = "● × $currentNumber"
            dot.textSize = 18f
            dot.setTextColor(colors[currentNumber % colors.size])
            binding.dotsContainer.addView(dot)
        }
    }

    // ==================== 玩法切换 ====================

    private fun setupModeSwitch() {
        binding.btnModeLearn.setOnClickListener { switchMode(0) }
        binding.btnModeFind.setOnClickListener {
            switchMode(1)
            startGame(GAME_FIND)
        }
        binding.btnModeCount.setOnClickListener {
            switchMode(1)
            startGame(GAME_COUNT)
        }
    }

    private fun switchMode(mode: Int) {
        binding.pageLearn.isVisible = mode == 0
        binding.pageGame.isVisible = mode == 1
        val learn = mode == 0
        binding.btnModeLearn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (learn) 0xFFFB8C00.toInt() else 0x55FFFFFF.toInt()
        )
        binding.btnModeFind.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (!learn && gameMode == GAME_FIND) 0xFFFB8C00.toInt() else 0x55FFFFFF.toInt()
        )
        binding.btnModeCount.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (!learn && gameMode == GAME_COUNT) 0xFFFB8C00.toInt() else 0x55FFFFFF.toInt()
        )
    }

    // ==================== 找数字 / 数一数 ====================

    private fun startGame(mode: Int) {
        gameMode = mode
        gameScore = 0
        binding.tvGameScore.text = "⭐ 得分: 0"
        binding.tvGameTitle.text = if (mode == GAME_FIND) {
            "🎯 听一听，找出正确的数字吧！"
        } else {
            "🔢 数一数，一共有几个？"
        }
        nextGameQuestion()
    }

    private fun nextGameQuestion() {
        val answer = if (gameMode == GAME_FIND) {
            Random.nextInt(0, maxNumber + 1)
        } else {
            Random.nextInt(1, 11)
        }
        gameAnswer = answer

        if (gameMode == GAME_FIND) {
            binding.tvGameQuestion.text = "👂"
            binding.tvGameQuestion.textSize = 90f
            KidsTts.speak("请你找出数字，${numberNames[answer]}")
        } else {
            // 数一数：随机一个多彩图标，显示 answer 个
            val icon = countIcons[Random.nextInt(countIcons.size)]
            binding.tvGameQuestion.text = buildString {
                repeat(answer) { append(icon.emoji) }
            }
            binding.tvGameQuestion.textSize = 44f
            binding.tvGameTitle.text = "🔢 数一数，一共有几个${icon.name}？"
            KidsTts.speak("数一数，一共有几个${icon.name}？")
        }

        val options = generateGameOptions(answer)
        binding.btnOpt1.text = options[0].toString()
        binding.btnOpt2.text = options[1].toString()
        binding.btnOpt3.text = options[2].toString()

        binding.tvGameFeedback.text = ""
        binding.btnGameNext.visibility = View.GONE
        enableGameOptions(true)
    }

    private fun generateGameOptions(answer: Int): IntArray {
        val maxVal = if (gameMode == GAME_FIND) maxNumber else 10
        val options = mutableSetOf(answer)
        while (options.size < 3) {
            options.add(Random.nextInt(0, maxVal + 1))
        }
        return options.shuffled().toIntArray()
    }

    private fun setupGameListeners() {
        binding.btnOpt1.setOnClickListener { checkGameAnswer(binding.btnOpt1.text.toString().toIntOrNull() ?: -1) }
        binding.btnOpt2.setOnClickListener { checkGameAnswer(binding.btnOpt2.text.toString().toIntOrNull() ?: -1) }
        binding.btnOpt3.setOnClickListener { checkGameAnswer(binding.btnOpt3.text.toString().toIntOrNull() ?: -1) }
        binding.btnGameNext.setOnClickListener { nextGameQuestion() }
    }

    private fun checkGameAnswer(selected: Int) {
        if (selected == gameAnswer) {
            gameScore += 10
            binding.tvGameScore.text = "⭐ 得分: $gameScore"
            binding.tvGameFeedback.text = praises[Random.nextInt(praises.size)]
            binding.tvGameFeedback.setTextColor(0xFF2E7D32.toInt())
            KidsTts.speak("${praises[Random.nextInt(praises.size)]} 是 ${numberNames[gameAnswer]}")
            binding.tvGameScore.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120)
                .withEndAction {
                    binding.tvGameScore.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
        } else {
            binding.tvGameFeedback.text = "❌ 正确答案是 ${numberNames[gameAnswer]} ($gameAnswer)"
            binding.tvGameFeedback.setTextColor(0xFFC62828.toInt())
            KidsTts.speak("正确答案是 ${numberNames[gameAnswer]}")
        }
        binding.btnGameNext.visibility = View.VISIBLE
        enableGameOptions(false)
    }

    private fun enableGameOptions(enable: Boolean) {
        binding.btnOpt1.isEnabled = enable
        binding.btnOpt2.isEnabled = enable
        binding.btnOpt3.isEnabled = enable
    }

    companion object {
        const val GAME_FIND = 0
        const val GAME_COUNT = 1

        fun start(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, NumberLearningActivity::class.java))
        }
    }
}
