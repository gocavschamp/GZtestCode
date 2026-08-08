package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityEnglishGameBinding
import com.example.firstapplication.ui.kids.EnglishLibrary.EnglishWord

/**
 * 英语乐园二级页④：互动游戏（100 关听音选词闯关）
 * 答对 +10 分并通关 +1，进度/得分 Room 持久化；内容超屏时上下滑动查看
 */
class EnglishGameActivity : EnglishBaseActivity() {

    private lateinit var binding: ActivityEnglishGameBinding

    // 游戏状态（100 关闯关，进度持久化）
    private val gamePool = EnglishLibrary.ALL_WORDS
    private var gameProgress = 0
    private var gameScore = 0
    private var currentQuestionWord: EnglishWord? = null
    private val gameButtons: List<Button> get() = listOf(
        binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4
    )

    /** 游戏选项按钮初始背景色（答错后恢复用） */
    private val optionTints = mutableListOf<android.content.res.ColorStateList?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }

        optionTints.clear()
        gameButtons.forEach { optionTints.add(it.backgroundTintList) }

        setupGamePage()
    }

    private fun setupGamePage() {
        binding.btnGameSpeak.setOnClickListener { speakCurrentQuestion() }
        gameButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { onAnswer(index) }
        }
        binding.btnGameRestart.setOnClickListener {
            gameProgress = 0
            gameScore = 0
            KidsProgressStore.setEnglishGameProgress(this, 0)
            KidsProgressStore.setEnglishGameScore(this, 0)
            binding.btnGameRestart.visibility = View.GONE
            binding.tvGameResult.text = ""
            showQuestion()
        }
        gameProgress = KidsProgressStore.getEnglishGameProgress(this).coerceIn(0, 100)
        gameScore = KidsProgressStore.getEnglishGameScore(this)
        showQuestion()
    }

    private fun showQuestion() {
        if (gameProgress >= 100) {
            showGameComplete()
            return
        }
        val word = gamePool[gameProgress % gamePool.size]
        currentQuestionWord = word
        val options = (gamePool.filter { it.word != word.word }.shuffled().take(3).map { it.word } + word.word).shuffled()
        binding.tvGameScore.text = "⭐ 累计 $gameScore 分"
        binding.tvGameProgress.text = "已通关 $gameProgress / 100 关"
        binding.tvGameEmoji.text = word.emoji
        binding.tvGameTip.text = "第 ${gameProgress + 1} 关 · 听发音选单词"
        gameButtons.forEachIndexed { i, btn ->
            btn.text = options[i]
            btn.isEnabled = true
            btn.alpha = 1f
            btn.backgroundTintList = optionTints[i]
        }
        speakCurrentQuestion()
    }

    private fun speakCurrentQuestion() {
        currentQuestionWord?.let { KidsTts.speakEnglish(it.word) }
    }

    private fun onAnswer(index: Int) {
        val word = currentQuestionWord ?: return
        val btn = gameButtons[index]
        if (btn.text.toString() == word.word) {
            // 答对：+10 分、通关 +1，进度持久化
            gameScore += 10
            gameProgress++
            KidsProgressStore.setEnglishGameScore(this, gameScore)
            KidsProgressStore.setEnglishGameProgress(this, gameProgress)
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kids_grass)
            KidsTts.speakEnglish(word.word)
            binding.tvGameScore.text = "⭐ 累计 $gameScore 分"
            binding.root.postDelayed({
                if (gameProgress >= 100) showGameComplete() else showQuestion()
            }, 650)
        } else {
            // 答错：按钮变红禁用，可重试
            btn.isEnabled = false
            btn.alpha = 0.4f
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kids_coral)
        }
    }

    private fun showGameComplete() {
        currentQuestionWord = null
        binding.tvGameResult.text = "🎉 恭喜通关全部 100 关！累计得分 $gameScore 分，你是英语小天才！"
        KidsTts.speakEnglish("Congratulations! You are an English star!")
        binding.btnGameRestart.visibility = View.VISIBLE
        binding.tvGameProgress.text = "已通关 100 / 100 关"
        binding.tvGameTip.text = "太棒了，全部通关！"
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishGameActivity::class.java))
        }
    }
}
