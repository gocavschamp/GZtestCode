package com.example.firstapplication.ui.kids

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 幼儿教育模块本地语音朗读（Android 系统 TextToSpeech 中文引擎）
 * 纯本地播放，无需联网；用于数字 / 汉字 / 题目朗读
 */
object KidsTts {

    private var tts: TextToSpeech? = null
    private var ready = false

    /** 初始化（全局调用一次即可，如 MyApp / 首页） */
    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINESE) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 幼儿语速稍慢，更清晰
                    tts?.setSpeechRate(0.85f)
                }
                result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                false
            }
        }
    }

    /** 朗读一段文字（打断上一句） */
    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kids_tts_${System.currentTimeMillis()}")
    }

    /** 英文朗读（临时切到美式英语，说完自动恢复中文，用于英语模块） */
    fun speakEnglish(text: String) {
        if (!ready || text.isBlank()) return
        tts?.language = Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kids_tts_en_${System.currentTimeMillis()}")
        tts?.language = Locale.CHINESE
    }

    /** 停止朗读 */
    fun stop() {
        tts?.stop()
    }

    /** 释放资源（Application 生命周期结束） */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
