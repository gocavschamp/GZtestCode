package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityNumberLearningBinding

/**
 * 数字认识模块：按难度展示数字 + 对应点数，动画展示，适合 3-9 岁
 * 难度 1：0-5   难度 2：0-10   难度 3：0-20
 */
class NumberLearningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberLearningBinding

    private var currentLevel = 1
    private var currentNumber = 0
    private var maxNumber = 5

    private val colors = intArrayOf(
        0xFFFFB74D.toInt(), 0xFFFF8A80.toInt(), 0xFFAED581.toInt(),
        0xFF80DEEA.toInt(), 0xFFD1C4E9.toInt(), 0xFFF48FB1.toInt(),
        0xFFFFD54F.toInt(), 0xFF81C784.toInt(), 0xFF4FC3F7.toInt(),
        0xFFBA68C8.toInt(), 0xFFA1887F.toInt()
    )

    private val numberNames = arrayOf(
        "零", "一", "二", "三", "四", "五",
        "六", "七", "八", "九", "十",
        "十一", "十二", "十三", "十四", "十五",
        "十六", "十七", "十八", "十九", "二十",
        "二十一", "二十二", "二十三", "二十四", "二十五",
        "二十六", "二十七", "二十八", "二十九", "三十"
    )

    /** 连续答对鼓励语 */
    private val praises = arrayOf("真棒！", "太厉害了！", "好聪明呀！", "加油！", "你真棒！")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumberLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.chipLevel1.isChecked = true
        setupLevelListeners()
        setupNavListeners()
        updateDisplay()
    }

    private fun setupLevelListeners() {
        binding.chipLevel1.setOnClickListener {
            currentLevel = 1
            maxNumber = 5
            currentNumber = 0
            updateDisplay()
        }
        binding.chipLevel2.setOnClickListener {
            currentLevel = 2
            maxNumber = 10
            currentNumber = 0
            updateDisplay()
        }
        binding.chipLevel3.setOnClickListener {
            currentLevel = 3
            maxNumber = 30
            currentNumber = 0
            updateDisplay()
        }
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
            .scaleX(1.15f).scaleY(1.15f)
            .setDuration(120)
            .withEndAction {
                binding.tvNumber.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
            .start()
        renderDots()
        KidsProgressStore.setMaxNumberLearned(this, currentNumber)
    }

    private fun renderDots() {
        binding.dotsContainer.removeAllViews()
        val n = currentNumber
        if (n == 0) {
            val tv = TextView(this)
            tv.text = "0"
            tv.textSize = 40f
            tv.setTextColor(colors[0])
            binding.dotsContainer.addView(tv)
            return
        }
        // 最多展示 10 个点，用 emoji 圆点 + 数字辅助
        val displayCount = if (n <= 10) n else 10
        for (i in 0 until displayCount) {
            val dot = TextView(this)
            dot.text = "●"
            dot.textSize = 18f
            dot.setTextColor(colors[i % colors.size])
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(4, 0, 4, 0)
            binding.dotsContainer.addView(dot, lp)
        }
        if (n > 10) {
            val tv = TextView(this)
            tv.text = "+${n - 10}"
            tv.textSize = 16f
            tv.setTextColor(colors[n % colors.size])
            binding.dotsContainer.addView(tv)
        }
        binding.dotsContainer.animate().alpha(0.3f).setDuration(80).withEndAction {
            binding.dotsContainer.animate().alpha(1f).setDuration(180)
                .setInterpolator(AccelerateInterpolator()).start()
        }.start()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, NumberLearningActivity::class.java))
        }
    }
}
