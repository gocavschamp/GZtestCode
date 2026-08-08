package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.databinding.ActivityNumberArithmeticBinding
import kotlin.random.Random

/**
 * 数字加减法模块：难度分级出题答题
 * 难度 1：10 以内   难度 2：20 以内   难度 3：100 以内（不进位/不借位）
 */
class NumberArithmeticActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberArithmeticBinding

    private var currentLevel = 1
    private var score = 0
    private var streak = 0
    private var bestStreak = 0

    /** 一局统计（10 题为一局，用于首页答题局数统计） */
    private var questionInRound = 0
    private var correctInRound = 0

    private var currentAnswer = 0
    private var answerOptions = intArrayOf(0, 0, 0)
    private var currentQuestionText = ""

    private val praises = arrayOf("真棒！", "太厉害了！", "好聪明呀！", "太棒了！", "你真棒！")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumberArithmeticBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)
        bestStreak = KidsProgressStore.getBestStreak(this)

        binding.chipLevel1.isChecked = true
        binding.tvBest.text = bestStreak.toString()

        // 本局进度：10 题一局
        binding.progressRound.max = 10
        binding.progressRound.progress = 0

        binding.chipLevel1.setOnClickListener { switchLevel(1) }
        binding.chipLevel2.setOnClickListener { switchLevel(2) }
        binding.chipLevel3.setOnClickListener { switchLevel(3) }

        binding.btnAns1.setOnClickListener { checkAnswer(binding.btnAns1.text.toString().toInt()) }
        binding.btnAns2.setOnClickListener { checkAnswer(binding.btnAns2.text.toString().toInt()) }
        binding.btnAns3.setOnClickListener { checkAnswer(binding.btnAns3.text.toString().toInt()) }
        binding.btnNext.setOnClickListener { nextQuestion() }
        binding.btnSpeakQuestion.setOnClickListener { speakQuestion() }

        nextQuestion()
    }

    private fun switchLevel(level: Int) {
        if (level > KidsProgressStore.getUnlockedLevel(this)) {
            Toast.makeText(this, "先在上一难度累积连胜 3 次解锁哦！", Toast.LENGTH_SHORT).show()
            return
        }
        currentLevel = level
        score = 0
        streak = 0
        questionInRound = 0
        correctInRound = 0
        updateScoreBar()
        nextQuestion()
    }

    private fun nextQuestion() {
        val range = when (currentLevel) {
            1 -> 10
            2 -> 20
            else -> 100
        }
        val isAdd = Random.nextBoolean()
        val (a, b) = generateOperands(range, isAdd)
        currentAnswer = if (isAdd) a + b else a - b
        questionInRound++

        binding.tvQuestion.text = "$a ${if (isAdd) "+" else "-"} $b = ?"
        currentQuestionText = "$a ${if (isAdd) "+" else "-"} $b"
        answerOptions = generateOptions(currentAnswer)
        binding.btnAns1.text = answerOptions[0].toString()
        binding.btnAns2.text = answerOptions[1].toString()
        binding.btnAns3.text = answerOptions[2].toString()

        binding.tvFeedback.text = ""
        binding.btnNext.visibility = android.view.View.GONE
        enableAnswers(true)
        updateScoreBar()

        // 出题语音朗读（本地 TTS）
        speakQuestion()
    }

    /** 生成不产生负数的操作数（低难度保证不进位/不借位，界面友好） */
    private fun generateOperands(range: Int, isAdd: Boolean): Pair<Int, Int> {
        return if (isAdd) {
            if (currentLevel == 3) {
                val a = Random.nextInt(1, 10) * 10
                val b = Random.nextInt(0, 10)
                a to b
            } else {
                val a = Random.nextInt(1, range)
                val b = Random.nextInt(0, range - a + 1)
                a to b
            }
        } else {
            if (currentLevel == 3) {
                val a = Random.nextInt(1, 10) * 10
                val b = Random.nextInt(0, 10)
                a to b
            } else {
                val a = Random.nextInt(1, range)
                val b = Random.nextInt(0, a + 1)
                a to b
            }
        }
    }

    private fun generateOptions(answer: Int): IntArray {
        val options = mutableSetOf<Int>()
        options.add(answer)
        while (options.size < 3) {
            val offset = Random.nextInt(1, 4) * (if (Random.nextBoolean()) 1 else -1)
            val candidate = answer + offset
            if (candidate >= 0) options.add(candidate)
        }
        return options.shuffled().toIntArray()
    }

    private fun checkAnswer(selected: Int) {
        if (selected == currentAnswer) {
            streak++
            score += 10
            correctInRound++
            binding.tvFeedback.text = praises[Random.nextInt(praises.size)]
            binding.tvFeedback.setTextColor(Color.parseColor("#2E7D32"))
            // 答对语音鼓励
            KidsTts.speak(binding.tvFeedback.text.toString())
            KidsProgressStore.recordArithmeticResult(this, streak)
            // 分数跳动动画
            binding.tvScore.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120)
                .withEndAction {
                    binding.tvScore.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
            // 难度解锁：连续答对 3 次解锁下一难度
            if (streak >= 3 && currentLevel < 3) {
                val next = currentLevel + 1
                if (KidsProgressStore.getUnlockedLevel(this) < next) {
                    KidsProgressStore.setUnlockedLevel(this, next)
                    Toast.makeText(this, "🎉 解锁了更高难度！", Toast.LENGTH_SHORT).show()
                    KidsTts.speak("恭喜，解锁了更高难度！")
                }
            }
            // 一局 10 题完成：记录一局并鼓励
            if (questionInRound >= 10) {
                KidsProgressStore.addArithmeticRecord(this, currentLevel, questionInRound, correctInRound, streak)
                val msg = "🎊 完成一局！答对 $correctInRound / $questionInRound 题"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                KidsTts.speak("完成一局，你真棒！")
                questionInRound = 0
                correctInRound = 0
            }
        } else {
            // 答错：若本局已答过题则结算一局
            if (questionInRound > 0) {
                KidsProgressStore.addArithmeticRecord(this, currentLevel, questionInRound, correctInRound, streak)
                questionInRound = 0
                correctInRound = 0
            }
            streak = 0
            binding.tvFeedback.text = "❌ 再想想哦，正确答案是 $currentAnswer"
            binding.tvFeedback.setTextColor(Color.parseColor("#C62828"))
        }
        bestStreak = KidsProgressStore.getBestStreak(this)
        updateScoreBar()
        binding.btnNext.visibility = android.view.View.VISIBLE
        enableAnswers(false)
    }

    /** 朗读当前题目（中文口语化） */
    private fun speakQuestion() {
        val spoken = currentQuestionText.replace("+", "加").replace("-", "减") + "等于几？"
        KidsTts.speak(spoken)
    }

    private fun updateScoreBar() {
        binding.tvScore.text = score.toString()
        binding.tvStreak.text = streak.toString()
        binding.tvBest.text = bestStreak.toString()
        binding.progressRound.progress = questionInRound.coerceAtMost(10)
    }

    private fun enableAnswers(enabled: Boolean) {
        binding.btnAns1.isEnabled = enabled
        binding.btnAns2.isEnabled = enabled
        binding.btnAns3.isEnabled = enabled
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, NumberArithmeticActivity::class.java))
        }
    }
}
