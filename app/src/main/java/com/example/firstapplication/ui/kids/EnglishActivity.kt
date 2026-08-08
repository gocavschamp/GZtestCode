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
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityEnglishBinding
import com.example.firstapplication.databinding.ItemEnglishCheckinDayBinding
import com.example.firstapplication.databinding.ItemEnglishFlashcardBinding
import com.example.firstapplication.databinding.ItemEnglishSentenceBinding
import com.example.firstapplication.ui.kids.EnglishLibrary.EnglishWord
import com.example.firstapplication.ui.kids.EnglishLibrary.Sentence
import java.util.Calendar

/**
 * 英语乐园：字母表 / 单词句子（闪卡切换）/ 每日打卡（30天×10词+2句，每词配 2 例句，本地记录）/ 互动游戏（100 关听音选词，进度持久化）
 * 朗读：字母、单词、句子均为英文发音（KidsTts.speakEnglish），中文释义按钮读中文
 */
class EnglishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnglishBinding

    private var currentLetterIndex = 0
    private var currentCategoryIndex = 0

    private val wordAdapter = WordAdapter()
    private val sentenceAdapter = SentenceAdapter()
    private val checkinAdapter = CheckinAdapter()
    private val checkinContentAdapter = CheckinContentAdapter()

    // 打卡状态
    private var checkinMask = 0
    private var selectedCheckinDay = 0
    private var currentCheckinPos = 0

    // 闪卡位置
    private var currentWordPos = 0

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

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 橙黄渐变浅色背景 → 深色状态栏图标
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        checkinMask = KidsProgressStore.getEnglishCheckinMask(this)
        selectedCheckinDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 30)

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
            speakLetter(EnglishLibrary.ALPHABET[currentLetterIndex])
        }
        binding.btnPrevLetter.setOnClickListener { moveLetter(-1) }
        binding.btnNextLetter.setOnClickListener { moveLetter(1) }
        showLetter(EnglishLibrary.ALPHABET[0])
    }

    /** 字母切换：上一个/下一个 */
    private fun moveLetter(delta: Int) {
        val next = (currentLetterIndex + delta).coerceIn(0, EnglishLibrary.ALPHABET.size - 1)
        if (next == currentLetterIndex) return
        currentLetterIndex = next
        refreshLetterChips()
        showLetter(EnglishLibrary.ALPHABET[next])
        val child = binding.alphabetList.getChildAt(next)
        binding.alphabetScroll.smoothScrollTo(child.left - 24, 0)
    }

    /** 刷新字母 chip 选中样式 */
    private fun refreshLetterChips() {
        for (i in 0 until binding.alphabetList.childCount) {
            val tv = binding.alphabetList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentLetterIndex)
            tv.setTextColor(if (i == currentLetterIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    private fun showLetter(info: EnglishLibrary.LetterInfo) {
        binding.tvLetterBig.text = "${info.letter} ${info.lowercase}"
        binding.tvLetterPhonetic.text = info.phonetic
        binding.letterWordList.removeAllViews()
        info.words.forEach { sample ->
            binding.letterWordList.addView(createLetterWordCard(sample.word, sample.chinese, sample.emoji))
        }
    }

    /** 字母页示例单词卡（复用闪卡布局，隐藏例句，固定高度） */
    private fun createLetterWordCard(word: String, chinese: String, emoji: String): View {
        val b = ItemEnglishFlashcardBinding.inflate(layoutInflater)
        b.tvFlashWord.text = word
        b.tvFlashPhonetic.visibility = View.GONE
        b.tvFlashChinese.text = chinese
        b.tvFlashEmoji.text = emoji
        b.viewFlashDivider.visibility = View.GONE
        b.flashExampleScroll.visibility = View.GONE
        b.btnSpeakFlash.visibility = View.GONE
        b.root.setOnClickListener { speakWord(word) }
        b.root.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(230)
        ).apply { topMargin = dp(10) }
        return b.root
    }

    private fun speakLetter(info: EnglishLibrary.LetterInfo) {
        KidsTts.speakEnglish(info.letter)
        binding.root.postDelayed({
            info.words.forEach { sample -> KidsTts.speakEnglish(sample.word) }
        }, 900)
    }

    private fun speakWord(word: String) {
        KidsTts.speakEnglish(word)
    }

    // ==================== 页面1：单词句子（闪卡切换） ====================

    private fun setupWordsPage() {
        EnglishLibrary.CATEGORIES.forEachIndexed { index, category ->
            val chip = createChip(category.name, index == 0)
            chip.setOnClickListener {
                currentCategoryIndex = index
                currentWordPos = 0
                refreshCategoryChips()
                binding.recyclerWords.adapter = wordAdapter
                wordAdapter.submitList(EnglishLibrary.CATEGORIES[index].words)
                binding.tvWordsHint.text = "✨ 左右滑动切换卡片 · 共 ${EnglishLibrary.CATEGORIES[index].words.size} 个单词"
            }
            binding.categoryList.addView(chip)
        }
        val sentenceChip = createChip("💬 常用句子", false)
        sentenceChip.setOnClickListener {
            currentCategoryIndex = -1
            currentWordPos = 0
            refreshCategoryChips()
            binding.recyclerWords.adapter = sentenceAdapter
            sentenceAdapter.submitList(EnglishLibrary.SENTENCES)
            binding.tvWordsHint.text = "✨ 左右滑动切换卡片 · 共 ${EnglishLibrary.SENTENCES.size} 句"
        }
        binding.categoryList.addView(sentenceChip)

        val lm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerWords.layoutManager = lm
        binding.recyclerWords.adapter = wordAdapter
        attachSnap(binding.recyclerWords) { pos, total -> "第 $pos / $total 张" }
        binding.btnPrevWord.setOnClickListener { moveWord(-1) }
        binding.btnNextWord.setOnClickListener { moveWord(1) }
        wordAdapter.submitList(EnglishLibrary.CATEGORIES[0].words)
        binding.tvWordsHint.text = "✨ 左右滑动切换卡片 · 共 ${EnglishLibrary.CATEGORIES[0].words.size} 个单词"
        binding.tvWordsIndicator.text = "第 1 / ${EnglishLibrary.CATEGORIES[0].words.size} 张"
    }

    private fun refreshCategoryChips() {
        for (i in 0 until binding.categoryList.childCount) {
            val tv = binding.categoryList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentCategoryIndex)
            tv.setTextColor(if (i == currentCategoryIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    /** 单词/句子闪卡切换：上一个/下一个 */
    private fun moveWord(delta: Int) {
        val total = binding.recyclerWords.adapter?.itemCount ?: 0
        if (total <= 0) return
        val next = (currentWordPos + delta).coerceIn(0, total - 1)
        if (next == currentWordPos) return
        currentWordPos = next
        binding.recyclerWords.smoothScrollToPosition(next)
        binding.tvWordsIndicator.text = "第 ${next + 1} / $total 张"
    }

    // ==================== 页面2：每日打卡 ====================

    private fun setupCheckinPage() {
        binding.recyclerCheckin.layoutManager = GridLayoutManager(this, 6)
        binding.recyclerCheckin.adapter = checkinAdapter
        binding.recyclerCheckin.isNestedScrollingEnabled = false
        checkinAdapter.submitList((1..30).toList())
        refreshCheckinHeader()

        binding.recyclerCheckinContent.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCheckinContent.adapter = checkinContentAdapter
        attachSnap(binding.recyclerCheckinContent) { pos, total -> "第 $pos / $total 张" }
        binding.btnPrevCheckin.setOnClickListener { moveCheckin(-1) }
        binding.btnNextCheckin.setOnClickListener { moveCheckin(1) }

        binding.btnCheckinDone.setOnClickListener {
            if (selectedCheckinDay <= 0) return@setOnClickListener
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            if (selectedCheckinDay != today) return@setOnClickListener
            checkinMask = KidsProgressStore.markEnglishCheckin(this, selectedCheckinDay)
            refreshCheckinHeader()
            checkinAdapter.notifyDataSetChanged()
            KidsTts.speakEnglish("Great job! You are awesome!")
            refreshCheckinContent()
        }

        // 默认选中今天
        showCheckinContent(selectedCheckinDay)
    }

    private fun refreshCheckinHeader() {
        val count = Integer.bitCount(checkinMask)
        binding.tvCheckinTitle.text = "📅 本月打卡 $count / 30 天"
    }

    /** 展示某天内容：10 个单词卡（各配 2 例句）+ 2 个句子卡，闪卡滑动切换 */
    private fun showCheckinContent(day: Int) {
        selectedCheckinDay = day
        currentCheckinPos = 0
        val plan = EnglishLibrary.CHECK_IN_DAYS[day - 1]
        val cards: MutableList<Any> = mutableListOf()
        cards.addAll(plan.words)
        cards.addAll(plan.sentences)
        checkinContentAdapter.submitList(cards)
        binding.recyclerCheckinContent.scrollToPosition(0)
        binding.tvCheckinIndicator.text = "第 1 / ${cards.size} 张"
        refreshCheckinContent()
    }

    /** 打卡内容闪卡切换：上一个/下一个 */
    private fun moveCheckin(delta: Int) {
        val total = binding.recyclerCheckinContent.adapter?.itemCount ?: 0
        if (total <= 0) return
        val next = (currentCheckinPos + delta).coerceIn(0, total - 1)
        if (next == currentCheckinPos) return
        currentCheckinPos = next
        binding.recyclerCheckinContent.smoothScrollToPosition(next)
        binding.tvCheckinIndicator.text = "第 ${next + 1} / $total 张"
    }

    /** 更新打卡按钮状态 */
    private fun refreshCheckinContent() {
        val day = selectedCheckinDay
        if (day <= 0) return
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val done = (checkinMask and (1 shl (day - 1))) != 0
        binding.btnCheckinDone.isEnabled = false
        binding.btnCheckinDone.alpha = 0.55f
        when {
            done -> {
                binding.btnCheckinDone.text = "✅ 第 $day 天已完成打卡"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_grass)
            }
            day == today -> {
                binding.btnCheckinDone.text = "✅ 完成今日打卡"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_grass)
                binding.btnCheckinDone.isEnabled = true
                binding.btnCheckinDone.alpha = 1f
            }
            day < today -> {
                binding.btnCheckinDone.text = "⏰ 第 $day 天已错过，今天起要坚持哦"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_coral)
            }
            else -> {
                binding.btnCheckinDone.text = "🔮 第 $day 天还没到"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_sky_blue_deep)
            }
        }
    }

    /** 绑定闪卡内容：单词 + 2 个例句 */
    private fun bindFlashcard(b: ItemEnglishFlashcardBinding, word: EnglishWord) {
        b.tvFlashWord.text = word.word
        b.tvFlashPhonetic.text = word.phonetic
        b.tvFlashChinese.text = word.chinese
        b.tvFlashEmoji.text = word.emoji
        b.flashExampleList.removeAllViews()
        EnglishLibrary.exampleSentencesFor(word).forEach { sentence ->
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 6)
                setOnClickListener { KidsTts.speakEnglish(sentence.en) }
            }
            block.addView(TextView(this).apply {
                text = sentence.en
                setTextColor(0xFF455A64.toInt())
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            block.addView(TextView(this).apply {
                text = "    ${sentence.cn}"
                setTextColor(0xFF78909C.toInt())
                textSize = 13f
            })
            b.flashExampleList.addView(block)
        }
        b.btnSpeakFlash.setOnClickListener { KidsTts.speakEnglish(word.word) }
        b.root.setOnClickListener { KidsTts.speakEnglish(word.word) }
    }

    // ==================== 页面3：互动游戏（100 关听音选词，进度持久化） ====================

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

    // ==================== 通用控件 ====================

    /** 闪卡 RecyclerView：一次一张，滑动吸附 + 指示器更新 */
    private fun attachSnap(rv: RecyclerView, formatter: (Int, Int) -> String) {
        val helper = LinearSnapHelper()
        helper.attachToRecyclerView(rv)
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rv.layoutManager as? LinearLayoutManager ?: return
                    val snap = helper.findSnapView(lm) ?: return
                    val pos = lm.getPosition(snap)
                    val total = rv.adapter?.itemCount ?: 0
                    when (rv) {
                        binding.recyclerWords -> {
                            currentWordPos = pos
                            binding.tvWordsIndicator.text = formatter(pos + 1, total)
                        }
                        binding.recyclerCheckinContent -> {
                            currentCheckinPos = pos
                            binding.tvCheckinIndicator.text = formatter(pos + 1, total)
                        }
                    }
                }
            }
        })
    }

    /** 圆角 chip：选中白底深字 / 未选中半透明深底白字 */
    private fun createChip(text: String, selected: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        tv.setTextColor(if (selected) 0xFF7A3C00.toInt() else Color.WHITE)
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
        if (selected) {
            setColor(Color.WHITE)
        } else {
            setColor(0x33000000)
            setStroke(1, 0x66FFFFFF.toInt())
        }
    }

    // ==================== Adapters ====================

    /** 单词闪卡 adapter（卡片含 2 个例句） */
    inner class WordAdapter : RecyclerView.Adapter<WordAdapter.VH>() {

        private var list = listOf<EnglishWord>()

        fun submitList(newList: List<EnglishWord>) {
            list = newList
            notifyDataSetChanged()
            binding.tvWordsIndicator.text = "第 1 / ${newList.size} 张"
        }

        inner class VH(val b: ItemEnglishFlashcardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishFlashcardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            bindFlashcard(holder.b, list[position])
        }
    }

    /** 句子闪卡 adapter */
    inner class SentenceAdapter : RecyclerView.Adapter<SentenceAdapter.VH>() {

        private var list = listOf<Sentence>()

        fun submitList(newList: List<Sentence>) {
            list = newList
            notifyDataSetChanged()
            binding.tvWordsIndicator.text = "第 1 / ${newList.size} 张"
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

    /** 打卡日历网格 adapter */
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
            b.tvCheckinDay.textSize = if (day == today) 16f else 14f
            // 选中日高亮描边
            val border = if (day == selectedCheckinDay) 3 else 0
            when {
                done -> {
                    b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishActivity, R.color.kids_grass))
                    b.tvCheckinStatus.text = "✓"
                    b.tvCheckinStatus.setTextColor(Color.WHITE)
                }
                day == today -> {
                    b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishActivity, R.color.kids_orange))
                    b.tvCheckinStatus.text = "今天"
                    b.tvCheckinStatus.setTextColor(0xE6FFFFFF.toInt())
                }
                day < today -> {
                    b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishActivity, R.color.kids_coral))
                    b.tvCheckinStatus.text = "✗"
                    b.tvCheckinStatus.setTextColor(Color.WHITE)
                }
                else -> {
                    b.root.setCardBackgroundColor(0x33000000)
                    b.tvCheckinStatus.text = ""
                }
            }
            b.root.strokeWidth = border
            b.root.strokeColor = Color.WHITE
            b.root.setOnClickListener { showCheckinContent(day) }
        }
    }

    /** 打卡内容闪卡 adapter：viewType 0=单词（flashcard 带 2 例句） 1=句子 */
    inner class CheckinContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var list = listOf<Any>()

        fun submitList(newList: List<Any>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int =
            if (list[position] is EnglishWord) 0 else 1

        override fun getItemCount(): Int = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == 0) {
                VH(ItemEnglishFlashcardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            } else {
                VHS(ItemEnglishSentenceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = list[position]) {
                is EnglishWord -> bindFlashcard((holder as VH).b, item)
                is Sentence -> {
                    val b = (holder as VHS).b
                    b.tvSentenceEn.text = item.en
                    b.tvSentenceCn.text = item.cn
                    b.root.setOnClickListener { KidsTts.speakEnglish(item.en) }
                    b.btnSpeakSentence.setOnClickListener { KidsTts.speakEnglish(item.en) }
                }
            }
        }

        inner class VH(val b: ItemEnglishFlashcardBinding) : RecyclerView.ViewHolder(b.root)
        inner class VHS(val b: ItemEnglishSentenceBinding) : RecyclerView.ViewHolder(b.root)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishActivity::class.java))
        }
    }
}
