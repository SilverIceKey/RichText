package com.sik.richtext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class LineNumberView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    /**
     * Line number paint
     * 行号画笔
     */
    private val lineNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Mark paint
     * 标记画笔
     */
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Line number data
     * 行号数据
     */
    private var lineNumberData: MutableList<LineNumberData> = mutableListOf()

    /**
     * Text size
     * 文本大小
     */
    private var textSize: Float = 50f

    /**
     * Mark color
     * 标记颜色
     */
    private var markColor: Int = Color.RED

    /**
     * On line click listener
     * 行号点击
     */
    private var onLineClickListener: (LineNumberData) -> Unit = {}

    /**
     * Max line width
     * 最大行号宽度
     */
    private var maxLineWidth: Float = 0f

    /**
     * Binding edit text
     * 绑定的文本编辑器
     */
    private var bindingEditText: RichEditText? = null

    /**
     * Line data num
     * 行数据量
     */
    private var markNum: MutableMap<Int, Int> = mutableMapOf()

    /**
     * Mark data
     * 标记点位数据
     */
    private var markData: MutableList<MarkData> = mutableListOf()

    /**
     * On mark change listener
     * 标记点修改监听
     */
    private var onMarkChangeListener: (MarkData) -> Unit = {}

    /**
     * Mark padding
     * 标记与行号间距
     */
    private val markPadding: Int = 10

    /**
     * Edit text scroll change listener
     * 文本编辑器滚动监听
     */
    private var editTextScrollChangeListener: MutableList<OnScrollChangeListener> = mutableListOf()

    init {
        lineNumberPaint.textSize = textSize

        markPaint.color = markColor
        markTextPaint.textSize = textSize * 0.6f
        markTextPaint.color = Color.WHITE
    }

    /**
     * Set data
     * 设置行数据
     * @param lineNumberData
     */
    private fun setLineData(lineNumberData: MutableList<LineNumberData>) {
        this.lineNumberData = lineNumberData
        maxLineWidth =
            lineNumberPaint.measureText("${lineNumberData[lineNumberData.size - 1].lineNumber}")
        val maxWidth =
            maxLineWidth + paddingLeft + paddingRight + textSize * 0.4 * 2 + 10
        val layoutParams = layoutParams
        layoutParams.width =
            maxWidth.toInt()
        this.layoutParams = layoutParams
        invalidate()
    }

    /**
     * Set data num
     * 设置行是有有内容数据
     * 行号 to 数量
     * @param dataNum
     */
    fun setDataNum(dataNum: MutableList<MarkData>) {
        this.markData = dataNum
        postDelayed({
            dataNumSet()
            postInvalidate()
        }, 200)
    }

    /**
     * Add data num
     * 添加数据
     * @param dataNum
     */
    fun addDataNum(markPosition: MarkData) {
        this.markData.add(markPosition)
        postDelayed({
            dataNumSet()
            postInvalidate()
        }, 200)
    }

    /**
     * Data num set
     * 数据设置
     */
    fun dataNumSet() {
        markNum.clear()
        markData.filter {
            val pageRange = bindingEditText?.getCurrentPagePosition() ?: intArrayOf(
                0, 0
            )
            it.position >= pageRange[0] && it.position <= pageRange[1]
        }.forEach {
            val line = bindingEditText?.getSelectionLine(it.position)
            markNum[line ?: 1] = (markNum[line] ?: 0) + 1
        }
        lineNumberData.filter { lineNum -> lineNum.dataNum != 0 }.forEach { it.dataNum = 0 }
        lineNumberData.filter { lineNum -> lineNum.lineNumber in markNum.map { it.key } }.forEach {
            it.dataNum = markNum[it.lineNumber] ?: 0
        }
    }

    /**
     * Mark change check
     * 标记变更检查
     */
    fun markChangeCheck(start: Int, before: Int, count: Int) {
        if (bindingEditText?.isPageMode() == true) {
            if (markData.isNotEmpty()) {
                if (count > before) {
                    markData.filter {
                        val pageRange = bindingEditText?.getCurrentPagePosition() ?: intArrayOf(
                            0, 0
                        )
                        it.position >= pageRange[0] && it.position <= pageRange[1]
                    }.forEach {
                        if (it.position > start + (bindingEditText?.getPageStartIndex() ?: 0)) {
                            it.position += count - before
                            onMarkChangeListener(it)
                        }
                    }
                } else if (count < before) {
                    markData.filter {
                        val pageRange = bindingEditText?.getCurrentPagePosition() ?: intArrayOf(
                            0, 0
                        )
                        it.position >= pageRange[0] && it.position <= pageRange[1]
                    }.forEach {
                        if (it.position > start + (bindingEditText?.getPageStartIndex() ?: 0)) {
                            it.position -= before - count
                            onMarkChangeListener(it)
                        }
                    }
                }
            }
        } else {
            if (markData.isNotEmpty()) {
                if (count > before) {
                    markData.forEach {
                        if (it.position > start) {
                            it.position += count - before
                            onMarkChangeListener(it)
                        }
                    }
                } else if (count < before) {
                    markData.forEach {
                        if (it.position > start) {
                            it.position -= before - count
                            onMarkChangeListener(it)
                        }
                    }
                }
            }
        }
        dataNumSet()
        postInvalidate()
    }

    /**
     * Get mark position
     * 获取标记位置
     * @return
     */
    fun getMarkPosition(): MutableList<MarkData> {
        return markData
    }

    /**
     * Set text size
     * 设置字体大小
     * @param textSize
     */
    private fun setTextSize(textSize: Float) {
        this.textSize = textSize
        lineNumberPaint.textSize = this.textSize
        markTextPaint.textSize = textSize * 0.6f
    }

    /**
     * Set mark color
     * 设置标记颜色
     * @param color
     */
    private fun setMarkColor(color: Int) {
        markColor = color
        markPaint.color = color
    }

    /**
     * Set on line click listener
     * 设置行点击
     * @param onLineClickListener
     * @receiver
     */
    fun setOnLineClickListener(onLineClickListener: (LineNumberData) -> Unit = {}) {
        this.onLineClickListener = onLineClickListener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var startY = paddingTop
        for (index in lineNumberData.indices) {
            val startX =
                maxLineWidth - lineNumberPaint.measureText("${lineNumberData[index].lineNumber}") + paddingLeft
            val textY =
                (lineNumberData[index].lineHeight - (lineNumberPaint.fontMetrics.descent - lineNumberPaint.fontMetrics.ascent)) / 2 - lineNumberPaint.fontMetrics.ascent
            canvas.drawText(
                "${lineNumberData[index].lineNumber}", startX, startY + textY, lineNumberPaint
            )
            if (lineNumberData[index].dataNum > 0) {
                canvas.drawCircle(
                    maxLineWidth + 5 + textSize * 0.4f + markPadding + paddingLeft,
                    lineNumberData[index].lineHeight / 2f + startY,
                    textSize * 0.4f,
                    markPaint
                )
                val markTextY =
                    lineNumberData[index].lineHeight / 2f + startY - markTextPaint.fontMetrics.ascent / 2.5f
                canvas.drawText(
                    "${lineNumberData[index].dataNum}",
                    maxLineWidth + 5 + textSize * 0.4f - (markTextPaint.measureText("${lineNumberData[index].dataNum}") / 2) + paddingLeft + markPadding,
                    markTextY,
                    markTextPaint
                )
            }
            lineNumberData[index].clickRect =
                Rect(0, startY + 1, width, startY + lineNumberData[index].lineHeight - 1)
            startY += lineNumberData[index].lineHeight
        }
    }

    /**
     * Add edit text scroll change listener
     * 添加文本滚动监听器
     * @param listener
     */
    fun addEditTextScrollChangeListener(listener: OnScrollChangeListener) {
        editTextScrollChangeListener.add(listener)
    }

    /**
     * Bind edit text
     * 绑定文本控件
     * @param editText
     */
    fun bindEditText(editText: RichEditText) {
        bindingEditText = editText
        layoutParams.apply {
            setPadding(
                editText.paddingLeft,
                editText.paddingTop,
                editText.paddingRight,
                editText.paddingBottom
            )
        }
        editText.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            scrollTo(0, scrollY)
        }
        editText.setOnTextSizeChangeListener { line ->
            updateLineNumbers(line)
        }
        updateLineNumbers()
    }

    /**
     * Update line numbers
     * 刷新行号
     */
    fun updateLineNumbers() {
        bindingEditText?.let {
            it.layout?.let { layout ->
                val lineCount = if (bindingEditText?.isPageMode() == true) {
                    RichEditText.DEFAULT_MAX_LINE_COUNT
                } else {
                    bindingEditText?.lineCount ?: 0
                }
                setPadding(
                    it.paddingLeft,
                    it.paddingTop,
                    it.paddingRight,
                    it.paddingBottom
                )
                setTextSize(it.textSize)
                setLineData(mutableListOf<LineNumberData>().apply {
                    for (index in 0 until lineCount) {
                        add(LineNumberData().apply {
                            lineNumber = index + 1
                            dataNum = markNum[index + 1] ?: 0
                            lineHeight =
                                layout.getLineBottom(index) - layout.getLineTop(index)
                        })
                    }
                })
            }
        }
    }

    /**
     * Update line numbers
     * 刷新行号
     * @param line
     */
    private fun updateLineNumbers(line: Int) {
        if (line == -1) {
            updateLineNumbers()
        } else {
            bindingEditText?.layout?.let {
                this.lineNumberData.filter { it.lineNumber == line }.forEach { item ->
                    item.apply {
                        lineHeight =
                            it.getLineBottom(lineNumber - 1) - it.getLineTop(lineNumber - 1)
                    }
                }
            }
            postInvalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            for (lineNumber in lineNumberData) {
                if (lineNumber.clickRect.contains(event.x.toInt(), event.y.toInt() + scrollY)) {
                    onLineClickListener(lineNumber)
                    break
                }
            }
        }
        return super.onTouchEvent(event)
    }
}