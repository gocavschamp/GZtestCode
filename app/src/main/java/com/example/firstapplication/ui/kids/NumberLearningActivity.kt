package com.example.firstapplication.ui.kids

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityNumberLearningBinding
import com.example.firstapplication.ui.kids.NumberLearningActivity.Companion.GAME_COUNT
import com.example.firstapplication.ui.kids.NumberLearningActivity.Companion.GAME_FIND
import kotlin.random.Random

/**
 * 数字乐园：认识数字（0-50）+ 找数字（听音选数）+ 数一数（点数）三种玩法
 */
class NumberLearningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberLearningBinding

    private var currentLevel = 1
    private var maxNumber = 5
    private var currentNumber = 0

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

    /** 数字中文名 0-50 */
    private val numberNames: Array<String> by lazy {
        val digits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val tens = arrayOf("", "十", "二十", "三十", "四十", "五十")
        Array(51) { n ->
            when {
                n <= 9 -> digits[n]
                n == 10 -> "十"
                n < 20 -> "十" + digits[n - 10]
                n % 10 == 0 -> tens[n / 10]
                else -> tens[n / 10] + digits[n % 10]
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumberLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLevelChips()
        setupNavListeners()
        setupModeSwitch()
        setupGameListeners()
        updateDisplay()
    }

    // ==================== 认识数字 ====================

    private fun setupLevelChips() {
        binding.chipLevel1.isChecked = true
        binding.chipLevel1.setOnClickListener {
            switchLevel(1, 5)
        }
        binding.chipLevel2.setOnClickListener {
            switchLevel(2, 10)
        }
        binding.chipLevel3.setOnClickListener {
            switchLevel(3, 50)
        }
    }

    private fun switchLevel(level: Int, max: Int) {
        currentLevel = level
        maxNumber = max
        currentNumber = 0
        updateDisplay()
    }

    private fun setupNavListeners() {
        binding.btnPrev.setOnClickListener {
            if (currentNumber > 0) {
                currentNumber--
                updateDisplay()
            }
        }
        binding.btnNext.setOnClickListener {
            if (currentNumber < maxNumber) {
                currentNumber++
                updateDisplay()
            }
        }
        // 点击数字卡片进入下一个数字
        binding.cardNumber.setOnClickListener {
            if (currentNumber < maxNumber) {
                currentNumber++
                updateDisplay()
            }
        }
        // 点击喇叭：朗读当前数字
        binding.btnSpeak.setOnClickListener {
            KidsTts.speak("${numberNames[currentNumber]}，${currentNumber}")
        }
    }

    private fun updateDisplay() {
        binding.tvNumber.text = currentNumber.toString()
        binding.tvNumber.setTextColor(colors[currentNumber % colors.size])
        binding.tvCountName.text = numberNames[currentNumber]
        binding.tvProgress.text = "第 ${currentNumber + 1} / ${maxNumber + 1} 个"
        // 切换数字时语音朗读（本地 TTS）
        KidsTts.speak(numberNames[currentNumber])
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
            "🍎 数一数，一共有几个苹果？"
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
            // 数一数：显示 answer 个苹果 emoji
            binding.tvGameQuestion.text = buildString {
                repeat(answer) { append("🍎") }
            }
            binding.tvGameQuestion.textSize = 44f
            KidsTts.speak("数一数，一共有几个苹果？")
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
