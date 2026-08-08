package com.example.firstapplication.ui.kids

import android.content.Context
import org.json.JSONObject

/**
 * 汉字笔画真实数据加载器
 *
 * 数据来源：hanzi-writer-data（Make Me a Hanzi 数据集，由 Arphic Public License 授权）
 * 资产文件 assets/hanzi_strokes.json，每个字包含笔画轮廓 s 与每笔画坐标集合 m，
 * 坐标已按 0-1024 归一化（SVG 坐标，绘制时需 Y 翻转，见 StrokeAnimationView）
 * m（medians）即每个笔画的坐标集合，绘制动画时按集合逐步 draw
 */
object HanziStrokeData {

    private const val ASSET = "hanzi_strokes.json"

    @Volatile
    private var root: JSONObject? = null

    /** 线程安全地加载一次（失败静默，避免崩溃，回退到旧数据逻辑） */
    fun load(context: Context) {
        if (root != null) return
        synchronized(this) {
            if (root != null) return
            root = try {
                val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
                JSONObject(text)
            } catch (e: Exception) {
                null
            }
        }
    }

    /** 该字是否已收录笔画数据 */
    fun has(char: String): Boolean = root?.has(char) == true

    /** 该字每笔画的 SVG 轮廓 path（与 medians 下标一一对应） */
    fun strokesOf(char: String): List<String>? {
        val arr = root?.optJSONObject(char)?.optJSONArray("s") ?: return null
        return List(arr.length()) { arr.getString(it) }
    }

    /** 该字每笔画的坐标集合（0-1024 归一化），下标与 strokes 对应 */
    fun mediansOf(char: String): List<List<FloatArray>>? {
        val arr = root?.optJSONObject(char)?.optJSONArray("m") ?: return null
        val result = ArrayList<List<FloatArray>>(arr.length())
        for (i in 0 until arr.length()) {
            val pts = arr.getJSONArray(i)
            val list = ArrayList<FloatArray>(pts.length())
            for (j in 0 until pts.length()) {
                val p = pts.getJSONArray(j)
                list.add(floatArrayOf(p.getDouble(0).toFloat(), p.getDouble(1).toFloat()))
            }
            result.add(list)
        }
        return result
    }
}
