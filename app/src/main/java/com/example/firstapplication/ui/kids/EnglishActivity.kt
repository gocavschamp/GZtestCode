package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityEnglishBinding
import com.example.firstapplication.databinding.ItemEnglishCheckinDayBinding
import com.example.firstapplication.databinding.ItemEnglishSentenceBinding
import com.example.firstapplication.databinding.ItemEnglishWordBinding
import com.example.firstapplication.ui.kids.EnglishLibrary.CheckInDay
import com.example.firstapplication.ui.kids.EnglishLibrary.EnglishWord
import com.example.firstapplication.ui.kids.EnglishLibrary.Sentence
import java.util.Calendar

/**
 * 英语乐园：字母表 / 单词句子 / 每日打卡（30天×10词+2句，本地记录）/ 互动游戏（听音选词）
 * 朗读：字母、单词、句子均为英文发音（KidsTts.speakEnglish），中文释义按钮读中文
 */
class EnglishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnglishBinding

    private var currentLetterIndex = 0
    private var currentCategoryIndex = 0

    private val wordAdapter = WordAdapter()
    private val sentenceAdapter = SentenceAdapter()
    private val checkinAdapter = CheckinAdapter()

    // 打卡状态
    private var checkinMask = 0

    // 游戏状态
    private val gamePool = EnglishLibrary.ALL_WORDS
    private var gameQuestions = mutableListOf<Pair<EnglishWord, List<String>>>()
    private var gameIndex = 0
    private var gameScore = 0
    private val gameButtons: List<Button> get() = listOf(
        binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4
    )

    /** 游戏选项按钮初始背景色（答错后恢复用） */
    private val optionTints = mutableListOf<android.content.res.ColorStateList?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 橙黄渐变浅色背景 → 深色状态栏图标
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        checkinMask = KidsProgressStore.getEnglishCheckinMask(this)

        optionTints.clear()
        gameButtons.forEach { optionTints.add(it.backgroundTintList) }

        setupTabs()
        setupAlphabetPage()
        setupWordsPage()
        setupCheckinPage()
        setupGamePage()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                binding.pageAlphabet.isVisible = tab.position == 0
                binding.pageWords.isVisible = tab.position == 1
                binding.pageCheckIn.isVisible = tab.position == 2
                binding.pageGame.isVisible = tab.position == 3
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    // ==================== 页面0：字母表 ====================

    private fun setupAlphabetPage() {
        EnglishLibrary.ALPHABET.forEachIndexed { index, info ->
            val chip = createChip(info.letter, index == 0)
            chip.setOnClickListener {
                currentLetterIndex = index
                refreshLetterChips()
                showLetter(info)
            }
            binding.alphabetList.addView(chip)
        }
        binding.btnSpeakLetter.setOnClickListener {
            val info = EnglishLibrary.ALPHABET[currentLetterIndex]
            speakLetter(info)
        }
        showLetter(EnglishLibrary.ALPHABET[0])
    }

    /** 刷新字母 chip 选中样式 */
    private fun refreshLetterChips() {
        for (i in 0 until binding.alphabetList.childCount) {
            (binding.alphabetList.getChildAt(i) as TextView).background =
                chipBackground(i == currentLetterIndex)
        }
    }

    private fun showLetter(info: EnglishLibrary.LetterInfo) {
        binding.tvLetterBig.text = "${info.letter} ${info.lowercase}"
        binding.tvLetterPhonetic.text = info.phonetic
        binding.letterWordList.removeAllViews()
        info.words.forEach { sample ->
            binding.letterWordList.addView(
                createLetterWordCard(sample.word, sample.chinese, sample.emoji)
            )
        }
    }

    /** 字母页示例单词卡片（点击朗读英文） */
    private fun createLetterWordCard(word: String, chinese: String, emoji: String): View {
        val b = ItemEnglishWordBinding.inflate(layoutInflater)
        b.tvWord.text = word
        b.tvWordChinese.text = chinese
        b.tvWordEmoji.text = emoji
        b.tvWordPhonetic.visibility = View.GONE
        b.root.setOnClickListener { speakWord(word) }
        b.btnSpeakWord.setOnClickListener { speakWord(word) }
        return b.root
    }

    private fun speakLetter(info: EnglishLibrary.LetterInfo) {
        // 字母名 + 示例单词一起朗读
        KidsTts.speakEnglish(info.letter)
        binding.root.postDelayed({
            info.words.forEach { sample -> KidsTts.speakEnglish(sample.word) }
        }, 900)
    }

    private fun speakWord(word: String) {
        KidsTts.speakEnglish(word)
    }

    // ==================== 页面1：单词句子 ====================

    private fun setupWordsPage() {
        // 分类 chips（最后加一个"常用句子"入口）
        EnglishLibrary.CATEGORIES.forEachIndexed { index, category ->
            val chip = createChip(category.name, index == 0)
            chip.setOnClickListener {
                currentCategoryIndex = index
                refreshCategoryChips()
                binding.recyclerWords.adapter = wordAdapter
                wordAdapter.submitList(EnglishLibrary.CATEGORIES[index].words)
                binding.tvWordsHint.text = "✨ 点击卡片听发音 · 共 ${EnglishLibrary.CATEGORIES[index].words.size} 个单词"
            }
            binding.categoryList.addView(chip)
        }
        val sentenceChip = createChip("💬 常用句子", false)
        sentenceChip.setOnClickListener {
            currentCategoryIndex = -1
            refreshCategoryChips()
            binding.recyclerWords.adapter = sentenceAdapter
            sentenceAdapter.submitList(EnglishLibrary.SENTENCES)
            binding.tvWordsHint.text = "✨ 点击卡片听句子 · 共 ${EnglishLibrary.SENTENCES.size} 句"
        }
        binding.categoryList.addView(sentenceChip)

        binding.recyclerWords.layoutManager = LinearLayoutManager(this)
        binding.recyclerWords.adapter = wordAdapter
        wordAdapter.submitList(EnglishLibrary.CATEGORIES[0].words)
        binding.tvWordsHint.text = "✨ 点击卡片听发音 · 共 ${EnglishLibrary.CATEGORIES[0].words.size} 个单词"
    }

    private fun refreshCategoryChips() {
        for (i in 0 until binding.categoryList.childCount) {
            (binding.categoryList.getChildAt(i) as TextView).background =
                chipBackground(i == currentCategoryIndex)
        }
    }

    // ==================== 页面2：每日打卡 ====================

    private fun setupCheckinPage() {
        binding.recyclerCheckin.layoutManager = GridLayoutManager(this, 6)
        binding.recyclerCheckin.adapter = checkinAdapter
        checkinAdapter.submitList((1..30).toList())
        refreshCheckinHeader()
    }

    private fun refreshCheckinHeader() {
        val count = Integer.bitCount(checkinMask)
        binding.tvCheckinTitle.text = "📅 本月打卡 $count / 30 天"
    }

    /** 展示某天内容：10 单词 + 2 句子 + 打卡按钮 */
    private fun showCheckinContent(day: Int) {
        val plan = EnglishLibrary.CHECK_IN_DAYS[day - 1]
        val container = binding.checkinContent
        container.removeAllViews()

        // 标题卡片
        val title = TextView(this).apply {
            text = "Day $day · ${plan.theme}"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 4)
        }
        container.addView(title)

        val sub = TextView(this).apply {
            text = "今天学 ${plan.words.size} 个单词 + ${plan.sentences.size} 个句子"
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }
        container.addView(sub)

        // 单词卡片（点击朗读英文）
        plan.words.forEach { word ->
            container.addView(createWordCard(word))
        }

        // 句子卡片
        plan.sentences.forEach { sentence ->
            container.addView(createSentenceCard(sentence))
        }

        // 打卡按钮
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val done = (checkinMask and (1 shl (day - 1))) != 0
        if (done) {
            container.addView(
                TextView(this).apply {
                    text = "✅ 第 $day 天已完成打卡，太棒啦！"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 18, 0, 12)
                }
            )
        } else if (day == today) {
            val btn = Button(this).apply {
                text = "✅ 完成今日打卡"
                textSize = 16f
                setTextColor(Color.WHITE)
                backgroundTintList = ContextCompat.getColorStateList(
                    this@EnglishActivity, R.color.kids_grass
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    120
                ).apply { topMargin = 14 }
            }
            btn.setOnClickListener {
                checkinMask = KidsProgressStore.markEnglishCheckin(this@EnglishActivity, day)
                refreshCheckinHeader()
                checkinAdapter.notifyDataSetChanged()
                KidsTts.speakEnglish("Great job! You are awesome!")
                showCheckinContent(day)
            }
            container.addView(btn)
        } else if (day < today) {
            container.addView(
                TextView(this).apply {
                    text = "⏰ 第 $day 天已过去，今天起要坚持打卡哦！"
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setPadding(0, 18, 0, 12)
                }
            )
        } else {
            container.addView(
                TextView(this).apply {
                    text = "🔮 第 $day 天还没到，先看看今天的内容吧！"
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setPadding(0, 18, 0, 12)
                }
            )
        }
    }

    /** 通用单词卡片（打卡页用，点击整卡朗读英文，喇叭读中文） */
    private fun createWordCard(word: EnglishWord): View {
        val b = ItemEnglishWordBinding.inflate(layoutInflater)
        b.tvWord.text = word.word
        b.tvWordPhonetic.text = word.phonetic
        b.tvWordChinese.text = word.chinese
        b.tvWordEmoji.text = word.emoji
        b.root.setOnClickListener { KidsTts.speakEnglish(word.word) }
        b.btnSpeakWord.setOnClickListener { KidsTts.speak(word.chinese) }
        return b.root
    }

    private fun createSentenceCard(sentence: Sentence): View {
        val b = ItemEnglishSentenceBinding.inflate(layoutInflater)
        b.tvSentenceEn.text = sentence.en
        b.tvSentenceCn.text = sentence.cn
        b.root.setOnClickListener { KidsTts.speakEnglish(sentence.en) }
        b.btnSpeakSentence.setOnClickListener { KidsTts.speakEnglish(sentence.en) }
        return b.root
    }

    // ==================== 页面3：互动游戏（听音选词） ====================

    private fun setupGamePage() {
        binding.btnGameSpeak.setOnClickListener { speakCurrentQuestion() }
        gameButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { onAnswer(index) }
        }
        binding.btnGameRestart.setOnClickListener { startGame() }
        startGame()
    }

    private fun startGame() {
        gameScore = 0
        gameIndex = 0
        // 10 题：随机 10 个正确词，各配 3 个干扰词
        gameQuestions = gamePool.shuffled().take(10).map { word ->
            val distractors = gamePool.filter { it.word != word.word }.shuffled().take(3).map { it.word }
            word to (distractors + word.word).shuffled()
        }.toMutableList()
        binding.btnGameRestart.visibility = View.GONE
        binding.tvGameResult.text = ""
        showQuestion()
    }

    private fun showQuestion() {
        binding.tvGameScore.text = "⭐ $gameScore 分"
        binding.tvGameProgress.text = "第 ${gameIndex + 1} / 10 题"
        val (word, options) = gameQuestions[gameIndex]
        binding.tvGameEmoji.text = word.emoji
        gameButtons.forEachIndexed { i, btn ->
            btn.text = options[i]
            btn.isEnabled = true
            btn.alpha = 1f
            btn.backgroundTintList = optionTints[i]
        }
        speakCurrentQuestion()
    }

    private fun speakCurrentQuestion() {
        val word = gameQuestions.getOrNull(gameIndex)?.first ?: return
        KidsTts.speakEnglish(word.word)
    }

    private fun onAnswer(index: Int) {
        val (word, _) = gameQuestions[gameIndex]
        val btn = gameButtons[index]
        if (btn.text.toString() == word.word) {
            // 答对：加分 + 绿色反馈
            gameScore++
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kids_grass)
            KidsTts.speakEnglish(word.word)
            binding.tvGameScore.text = "⭐ $gameScore 分"
            gameIndex++
            binding.root.postDelayed({
                if (gameIndex >= 10) finishGame() else showQuestion()
            }, 650)
        } else {
            // 答错：按钮变红禁用，可重试
            btn.isEnabled = false
            btn.alpha = 0.4f
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kids_coral)
        }
    }

    private fun finishGame() {
        binding.tvGameResult.text = if (gameScore >= 8) {
            "🎉 太棒了！10 题答对 $gameScore 题，你是英语小天才！"
        } else if (gameScore >= 5) {
            "😊 不错哦！10 题答对 $gameScore 题，继续加油！"
        } else {
            "💪 10 题答对 $gameScore 题，再练一练就会更好！"
        }
        KidsTts.speakEnglish("Game over! You got $gameScore.")
        binding.btnGameRestart.visibility = View.VISIBLE
        binding.tvGameProgress.text = "第 10 / 10 题"
    }

    // ==================== 通用控件 ====================

    /** 圆形圆角 chip（选中深色底白字 / 未选中半透明底白字） */
    private fun createChip(text: String, selected: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.background = chipBackground(selected)
        tv.setPadding(22, 12, 22, 12)
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 10
        tv.layoutParams = lp
        return tv
    }

    private fun chipBackground(selected: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 22f
        setColor(if (selected) 0x99FFFFFF.toInt() else 0x33000000)
        if (!selected) {
            setStroke(1, 0x66FFFFFF.toInt())
        }
    }

    // ==================== Adapters ====================

    inner class WordAdapter : RecyclerView.Adapter<WordAdapter.VH>() {

        private var list = listOf<EnglishWord>()

        fun submitList(newList: List<EnglishWord>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val b: ItemEnglishWordBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishWordBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val word = list[position]
            val b = holder.b
            b.tvWord.text = word.word
            b.tvWordPhonetic.text = word.phonetic
            b.tvWordChinese.text = word.chinese
            b.tvWordEmoji.text = word.emoji
            b.root.setOnClickListener { KidsTts.speakEnglish(word.word) }
            b.btnSpeakWord.setOnClickListener { KidsTts.speak(word.chinese) }
        }
    }

    inner class SentenceAdapter : RecyclerView.Adapter<SentenceAdapter.VH>() {

        private var list = listOf<Sentence>()

        fun submitList(newList: List<Sentence>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val b: ItemEnglishSentenceBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishSentenceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sentence = list[position]
            val b = holder.b
            b.tvSentenceEn.text = sentence.en
            b.tvSentenceCn.text = sentence.cn
            b.root.setOnClickListener { KidsTts.speakEnglish(sentence.en) }
            b.btnSpeakSentence.setOnClickListener { KidsTts.speakEnglish(sentence.en) }
        }
    }

    inner class CheckinAdapter : RecyclerView.Adapter<CheckinAdapter.VH>() {

        private var list = listOf<Int>()

        fun submitList(newList: List<Int>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val b: ItemEnglishCheckinDayBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishCheckinDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val day = list[position]
            val b = holder.b
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val done = (checkinMask and (1 shl (day - 1))) != 0
            b.tvCheckinDay.text = day.toString()
            b.tvCheckinDay.textSize = if (day == today) 20f else 17f
            if (done) {
                b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishActivity, R.color.kids_grass))
                b.tvCheckinStatus.text = "✓"
                b.tvCheckinStatus.setTextColor(Color.WHITE)
            } else if (day == today) {
                b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishActivity, R.color.kids_orange))
                b.tvCheckinStatus.text = "今天"
                b.tvCheckinStatus.setTextColor(0xE6FFFFFF.toInt())
            } else if (day < today) {
                b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishActivity, R.color.kids_coral))
                b.tvCheckinStatus.text = "✗"
                b.tvCheckinStatus.setTextColor(Color.WHITE)
            } else {
                b.root.setCardBackgroundColor(0x33000000)
                b.tvCheckinStatus.text = ""
            }
            b.root.setOnClickListener { showCheckinContent(day) }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishActivity::class.java))
        }
    }
}
