package com.example.firstapplication.ui.kids

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * 汉字仿写描红 View
 * 显示田字格与淡灰色底字，儿童用手指在底字上描摹，笔迹为彩色
 * 本地保存为 PNG 到 filesDir/kids_tracing/
 */
class TracingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentChar: String = "一"

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 2f
    }
    private val gridSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEEEEEE.toInt()
        strokeWidth = 1.5f
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x404FC3F7.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7043.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val inkPath = Path()
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private var lastX = 0f
    private var lastY = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setCharacter(char: String) {
        currentChar = char
        clearInk()
        invalidate()
    }

    fun getCharacter(): String = currentChar

    fun clearInk() {
        inkPath.reset()
        bitmap?.let {
            it.eraseColor(Color.TRANSPARENT)
        }
        invalidate()
    }

    /** 保存当前描红作品到本地，返回文件路径或 null */
    fun saveToLocal(): String? {
        val saved = bitmap ?: createBitmap() ?: return null
        return try {
            val dir = File(context.filesDir, "kids_tracing").apply { mkdirs() }
            val file = File(dir, "${currentChar}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                saved.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun createBitmap(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap = bmp
        bitmapCanvas = Canvas(bmp)
        return bmp
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createBitmap()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h) * 0.92f
        val left = (w - size) / 2f
        val top = (h - size) / 2f

        // 田字格
        canvas.save()
        canvas.translate(left, top)
        canvas.drawRect(0f, 0f, size, size, gridPaint)
        canvas.drawLine(size / 2, 0f, size / 2, size, gridSubPaint)
        canvas.drawLine(0f, size / 2, size, size / 2, gridSubPaint)

        // 底字（灰色提示）
        guidePaint.textSize = size * 0.85f
        val baseline = size / 2f - (guidePaint.ascent() + guidePaint.descent()) / 2f
        canvas.drawText(currentChar, size / 2f, baseline, guidePaint)
        canvas.restore()

        // 笔迹层
        bitmapCanvas?.let { bc ->
            bc.drawPath(inkPath, inkPaint)
            canvas.drawBitmap(bitmap!!, 0f, 0f, null)
        } ?: canvas.drawPath(inkPath, inkPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                inkPath.moveTo(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                inkPath.quadTo(lastX, lastY, (lastX + event.x) / 2f, (lastY + event.y) / 2f)
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
