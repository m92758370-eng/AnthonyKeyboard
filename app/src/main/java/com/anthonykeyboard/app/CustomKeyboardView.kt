package com.anthonykeyboard.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CustomKeyboardView(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    interface Listener {
        fun onCommitText(text: String)
        fun onBackspace()
        fun onEnter()
        fun onSpace()
        fun onSpaceLongPress()
        fun onAutoTypeButton()
        fun onPauseResumeButton()
    }

    var listener: Listener? = null

    private enum class KeyType { LETTER, SPACE, BACKSPACE, ENTER, LANG_SWITCH, AUTOTYPE, PAUSE_RESUME }

    private data class KeyRect(
        val label: String,
        val rect: RectF,
        val type: KeyType
    )

    companion object {
        private const val SPACE_LONG_PRESS_MS = 2000L
        private const val BACKSPACE_INITIAL_DELAY_MS = 400L
    }

    private var usePersian = true
    private val keys = mutableListOf<KeyRect>()

    private val keyPaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        isAntiAlias = true
    }
    private val specialKeyPaint = Paint().apply {
        color = Color.parseColor("#E8E8E8")
        isAntiAlias = true
    }
    private val accentPaint = Paint().apply {
        color = Color.parseColor("#4A90E2")
        isAntiAlias = true
    }
    private val highlightPaint = Paint().apply {
        color = Color.parseColor("#FFD54F")
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.parseColor("#222222")
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#DDDDDD")
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private var rowHeight = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var highlightedLabel: String? = null

    private var spacePressed = false
    private var spaceLongPressTriggered = false
    private val spaceLongPressRunnable = Runnable {
        spaceLongPressTriggered = true
        listener?.onSpaceLongPress()
    }

    private var backspacePressed = false
    private var backspaceRepeatCount = 0
    private val backspaceRunnable = object : Runnable {
        override fun run() {
            if (!backspacePressed) return
            listener?.onBackspace()
            backspaceRepeatCount++
            val nextDelay = when {
                backspaceRepeatCount < 8 -> 90L
                backspaceRepeatCount < 16 -> 60L
                backspaceRepeatCount < 24 -> 40L
                else -> 25L
            }
            handler.postDelayed(this, nextDelay)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightPx = (280 * context.resources.displayMetrics.density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, heightPx)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildKeys(w, h)
    }

    fun setLanguage(persian: Boolean) {
        usePersian = persian
        rebuildKeys(width, height)
        invalidate()
    }

    fun isPersian(): Boolean = usePersian

    private fun rebuildKeys(w: Int, h: Int) {
        keys.clear()
        if (w == 0 || h == 0) return

        val letterRows = if (usePersian) KeyboardLayouts.PERSIAN else KeyboardLayouts.ENGLISH
        val totalRows = letterRows.size + 1
        rowHeight = h.toFloat() / totalRows

        for ((rowIndex, row) in letterRows.withIndex()) {
            val keyWidth = w.toFloat() / row.size
            val top = rowHeight * rowIndex
            val bottom = top + rowHeight
            for ((colIndex, label) in row.withIndex()) {
                val left = keyWidth * colIndex
                val right = left + keyWidth
                keys.add(KeyRect(label, RectF(left, top, right, bottom), KeyType.LETTER))
            }
        }

        val bottomTop = rowHeight * letterRows.size
        val bottomBottom = bottomTop + rowHeight
        val switchW = w * 0.13f
        val pauseW = w * 0.11f
        val autoW = w * 0.13f
        val backW = w * 0.15f
        val enterW = w * 0.15f
        val spaceW = w - switchW - pauseW - autoW - backW - enterW

        var x = 0f
        keys.add(KeyRect(if (usePersian) "EN" else "فا", RectF(x, bottomTop, x + switchW, bottomBottom), KeyType.LANG_SWITCH))
        x += switchW
        keys.add(KeyRect("▶", RectF(x, bottomTop, x + pauseW, bottomBottom), KeyType.PAUSE_RESUME))
        x += pauseW
        keys.add(KeyRect("▶", RectF(x, bottomTop, x + autoW, bottomBottom), KeyType.AUTOTYPE))
        x += autoW
        keys.add(KeyRect("کینگ تایرکس", RectF(x, bottomTop, x + spaceW, bottomBottom), KeyType.SPACE))
        x += spaceW
        keys.add(KeyRect("⌫", RectF(x, bottomTop, x + backW, bottomBottom), KeyType.BACKSPACE))
        x += backW
        keys.add(KeyRect("⏎", RectF(x, bottomTop, x + enterW, bottomBottom), KeyType.ENTER))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#F5F5F5"))

        textPaint.textSize = rowHeight * 0.4f

        for (key in keys) {
            val paint = when {
                key.label == highlightedLabel -> highlightPaint
                key.type == KeyType.ENTER -> accentPaint
                key.type == KeyType.LETTER -> keyPaint
                else -> specialKeyPaint
            }
            val pad = 3f
            val rect = RectF(key.rect.left + pad, key.rect.top + pad, key.rect.right - pad, key.rect.bottom - pad)
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            canvas.drawRoundRect(rect, 10f, 10f, borderPaint)

            val cx = key.rect.centerX()
            val cy = key.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2

            val usedTextPaint = if (key.type == KeyType.ENTER) {
                Paint(textPaint).apply { color = Color.WHITE }
            } else {
                textPaint
            }

            if (key.type == KeyType.SPACE) {
                val spaceTextPaint = Paint(usedTextPaint).apply {
                    textSize = rowHeight * 0.22f
                }
                canvas.drawText(key.label, cx, cy, spaceTextPaint)
            } else {
                canvas.drawText(key.label, cx, cy, usedTextPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val key = keys.firstOrNull { it.rect.contains(event.x, event.y) } ?: return true
                when (key.type) {
                    KeyType.SPACE -> {
                        spacePressed = true
                        spaceLongPressTriggered = false
                        handler.postDelayed(spaceLongPressRunnable, SPACE_LONG_PRESS_MS)
                    }
                    KeyType.BACKSPACE -> {
                        backspacePressed = true
                        backspaceRepeatCount = 0
                        listener?.onBackspace()
                        handler.postDelayed(backspaceRunnable, BACKSPACE_INITIAL_DELAY_MS)
                    }
                    else -> dispatchKey(key)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (spacePressed) {
                    handler.removeCallbacks(spaceLongPressRunnable)
                    if (!spaceLongPressTriggered) {
                        listener?.onSpace()
                    }
                    spacePressed = false
                }
                if (backspacePressed) {
                    handler.removeCallbacks(backspaceRunnable)
                    backspacePressed = false
                }
            }
        }
        return true
    }

    private fun dispatchKey(key: KeyRect) {
        when (key.type) {
            KeyType.SPACE -> listener?.onSpace()
            KeyType.BACKSPACE -> listener?.onBackspace()
            KeyType.ENTER -> listener?.onEnter()
            KeyType.LANG_SWITCH -> setLanguage(!usePersian)
            KeyType.AUTOTYPE -> listener?.onAutoTypeButton()
            KeyType.PAUSE_RESUME -> listener?.onPauseResumeButton()
            KeyType.LETTER -> handleLetterTap(key.label)
        }
    }

    private fun handleLetterTap(label: String) {
        flashKey(label)
        listener?.onCommitText(label)
    }

    private fun flashKey(label: String) {
        highlightedLabel = label
        invalidate()
        handler.postDelayed({
            if (highlightedLabel == label) {
                highlightedLabel = null
                invalidate()
            }
        }, 120)
    }

    fun highlightKey(char: String) {
        flashKey(char)
    }
}
