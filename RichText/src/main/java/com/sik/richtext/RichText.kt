package com.sik.richtext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import org.wordpress.aztec.AztecText

open class RichText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet
) : AztecText(context, attrs) {
    private val mBackgroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRect = Rect()

    /**
     * Show background line
     * 是否显示横线背景
     */
    private var showBackgroundLine = false

    /**
     * On text size change listener
     * 文本大小改变监听
     */
    private var onTextSizeChangeListener: () -> Unit = {}

    init {
        mBackgroundLinePaint.style = Paint.Style.STROKE
        mBackgroundLinePaint.color = Color.BLACK  // 设置线的颜色
    }

    override fun onDraw(canvas: Canvas) {
        if (showBackgroundLine) {
            val count = lineCount
            val r = mRect
            val paint = mBackgroundLinePaint
            var startY = paddingTop
            for (index in 0 until count) {
                getLineBounds(index, r)
                val indexLineHeight = layout.getLineBottom(index) - layout.getLineTop(index)
                canvas.drawLine(
                    r.left.toFloat(),
                    startY + indexLineHeight.toFloat(),
                    r.right.toFloat(),
                    startY + indexLineHeight.toFloat(),
                    paint
                )
                startY += indexLineHeight
            }
        }
        super.onDraw(canvas)
    }

    /**
     * Set show background line
     * 设置是否显示横线背景
     * @param showBackgroundLine
     */
    fun setShowBackgroundLine(showBackgroundLine: Boolean) {
        this.showBackgroundLine = showBackgroundLine
        postInvalidate()
    }

    /**
     * Check show background line
     * 切换是否展示横线背景
     */
    fun switchShowBackgroundLine() {
        this.showBackgroundLine = !this.showBackgroundLine
        postInvalidate()
    }

    /**
     * Set on text size change listener
     * 设置文本大小改变监听
     * @param onTextSizeChangeListener
     * @receiver
     */
    fun setOnTextSizeChangeListener(onTextSizeChangeListener: () -> Unit = {}) {
        this.onTextSizeChangeListener = onTextSizeChangeListener
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        onTextSizeChangeListener()
    }
}