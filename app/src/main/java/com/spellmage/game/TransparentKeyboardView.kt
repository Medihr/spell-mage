package com.spellmage.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TransparentKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onKeyTypedListener: ((Char) -> Unit)? = null
    var onKeyActionListener: ((String) -> Unit)? = null

    private val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ñ"),
        listOf("CLR", "Z", "X", "C", "V", "B", "N", "M", "⌫")
    )

    private val keyRects = mutableMapOf<String, RectF>()
    private var activePressedKey: String? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        keyRects.clear()

        // 1. Draw Semi-Transparent Glass Backdrop
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = Color.argb(110, 15, 10, 25)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Top Border Line
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 3f
        borderPaint.color = Color.argb(180, 255, 215, 0)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, borderPaint)

        // 2. Render Key Grid
        val numRows = rows.size
        val rowHeight = (height - 24f) / numRows
        val margin = 6f

        for (r in 0 until numRows) {
            val keys = rows[r]
            val topY = margin + r * rowHeight

            var totalUnits = 0f
            for (k in keys) {
                totalUnits += if (k == "CLR" || k == "⌫") 1.5f else 1.0f
            }

            val unitWidth = (width - (keys.size + 1) * margin) / totalUnits
            var currentX = margin

            for (k in keys) {
                val keyW = if (k == "CLR" || k == "⌫") unitWidth * 1.5f else unitWidth
                val rect = RectF(currentX, topY, currentX + keyW, topY + rowHeight - margin)
                keyRects[k] = rect

                val isPressed = (activePressedKey == k)

                // Key Fill
                keyPaint.style = Paint.Style.FILL
                keyPaint.color = if (isPressed) Color.argb(200, 255, 215, 0) else Color.argb(45, 255, 255, 255)
                canvas.drawRoundRect(rect, 14f, 14f, keyPaint)

                // Key Border
                borderPaint.style = Paint.Style.STROKE
                borderPaint.strokeWidth = 2f
                borderPaint.color = if (isPressed) Color.YELLOW else Color.argb(90, 255, 255, 255)
                canvas.drawRoundRect(rect, 14f, 14f, borderPaint)

                // Key Text
                textPaint.color = if (isPressed) Color.BLACK else Color.WHITE
                textPaint.textSize = if (k == "CLR" || k == "⌫") 32f else 42f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textAlign = Paint.Align.CENTER
                val fontMetrics = textPaint.fontMetrics
                val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f

                canvas.drawText(k, rect.centerX(), baseline, textPaint)

                currentX += keyW + margin
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for ((key, rect) in keyRects) {
                    if (rect.contains(x, y)) {
                        activePressedKey = key
                        invalidate()

                        if (key == "CLR" || key == "⌫") {
                            onKeyActionListener?.invoke(key)
                        } else if (key.length == 1) {
                            onKeyTypedListener?.invoke(key[0])
                        }
                        break
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePressedKey = null
                invalidate()
            }
        }
        return true
    }
}
