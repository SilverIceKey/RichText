package com.sik.richtext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.sik.richtext.RichEditTextFormat.FORMAT_ALIGN_CENTER
import com.sik.richtext.RichEditTextFormat.FORMAT_ALIGN_LEFT
import com.sik.richtext.RichEditTextFormat.FORMAT_ALIGN_RIGHT
import com.sik.richtext.RichEditTextFormat.FORMAT_BOLD
import com.sik.richtext.RichEditTextFormat.FORMAT_FONT_COLOR
import com.sik.richtext.RichEditTextFormat.FORMAT_FONT_SIZE
import com.sik.richtext.RichEditTextFormat.FORMAT_HEADING_1
import com.sik.richtext.RichEditTextFormat.FORMAT_HEADING_2
import com.sik.richtext.RichEditTextFormat.FORMAT_HEADING_3
import com.sik.richtext.RichEditTextFormat.FORMAT_HEADING_4
import com.sik.richtext.RichEditTextFormat.FORMAT_HEADING_5
import com.sik.richtext.RichEditTextFormat.FORMAT_HEADING_REMOVE
import com.sik.richtext.RichEditTextFormat.FORMAT_ITALIC
import com.sik.richtext.RichEditTextFormat.FORMAT_STRIKETHROUGH
import com.sik.richtext.RichEditTextFormat.FORMAT_UNDERLINE

open class RichEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet
) : AppCompatEditText(context, attrs) {
    private val mBackgroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRect = Rect()

    /**
     * Spannable string builder
     * 样式文本
     */
    private var spannableStringBuilder: SpannableStringBuilder = SpannableStringBuilder()

    /**
     * Show background line
     * 是否显示横线背景
     */
    private var showBackgroundLine = false

    /**
     * On text size change listener
     * 文本大小改变监听
     */
    private var onTextSizeChangeListener: (line: Int) -> Unit = {}

    /**
     * Is bold enable
     * 是否开启加粗
     */
    private var isBoldEnable: Boolean = false

    /**
     * Is italic enable
     * 是否开启斜体
     */
    private var isItalicEnable: Boolean = false

    /**
     * Font color
     * 字体颜色
     */
    private var fontColor: Int = Color.parseColor("#414141")

    /**
     * Font size
     * 字体大小
     */
    private var fontSize: Float = textSize

    /**
     * Start input
     * 开始输入的位置
     */
    private var startInput: Int = 0

    /**
     * Count input
     * 总输入的字符
     */
    private var countInput: Int = 0

    /**
     * Is style change
     * 样式修改
     */
    var isStyleChange: Boolean = false

    init {
        mBackgroundLinePaint.style = Paint.Style.STROKE
        mBackgroundLinePaint.color = Color.BLACK  // 设置线的颜色
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                startInput = start
                countInput = count
            }

            override fun afterTextChanged(s: Editable) {
                if (!isStyleChange) {
                    isStyleChange = true
                    val finalSelectionStart = selectionStart
                    if (spannableStringBuilder != text) {
                        spannableStringBuilder = SpannableStringBuilder(text)
                    }
                    for (index in startInput until startInput + countInput) {
                        checkAndSetSpan(index)
                    }
                    text = spannableStringBuilder
                    setSelection(finalSelectionStart)
                    isStyleChange = false
                }
            }

        }
        addTextChangedListener(textWatcher)
    }

    private fun checkAndSetSpan(start: Int) {
        if (isBoldEnable || isItalicEnable) {
            spannableStringBuilder.setSpan(
                StyleSpan(
                    if (isBoldEnable && isItalicEnable) {
                        Typeface.BOLD_ITALIC
                    } else if (isBoldEnable) {
                        Typeface.BOLD
                    } else {
                        Typeface.ITALIC
                    }
                ), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        spannableStringBuilder.setSpan(
            AbsoluteSizeSpan(
                fontSize.toInt()
            ), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableStringBuilder.setSpan(
            ForegroundColorSpan(
                fontColor
            ), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
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
    fun setOnTextSizeChangeListener(onTextSizeChangeListener: (line: Int) -> Unit = {}) {
        this.onTextSizeChangeListener = onTextSizeChangeListener
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        onTextSizeChangeListener(-1)
    }

    /**
     * Format
     * 格式化
     * @param richEditTextFormat
     * @param value
     */
    @JvmOverloads
    fun format(richEditTextFormat: RichEditTextFormat, value: Any? = null) {
        isStyleChange = true
        val finalSelectionStart = selectionStart
        val finalSelectionEnd = selectionEnd
        if (spannableStringBuilder != text) {
            spannableStringBuilder = SpannableStringBuilder(text)
        }
        when (richEditTextFormat) {
            FORMAT_HEADING_1 -> {
                setHeading(1.5f)
            }

            FORMAT_HEADING_2 -> {
                setHeading(1.4f)
            }

            FORMAT_HEADING_3 -> {
                setHeading(1.3f)
            }

            FORMAT_HEADING_4 -> {
                setHeading(1.2f)
            }
            FORMAT_HEADING_5 -> {
                setHeading(1.1f)
            }

            FORMAT_HEADING_REMOVE -> {
                val blockPosition = getBlockPosition()
                removeSpan(
                    blockPosition.first, blockPosition.second, StyleSpan::class.java
                )
                removeSpan(
                    blockPosition.first, blockPosition.second, RelativeSizeSpan::class.java
                )
            }

            FORMAT_BOLD -> {
                isBoldEnable = !isBoldEnable
                if (selectionStart != selectionEnd) {
                    val boldStyleSpan =
                        getSpans(selectionStart, selectionEnd, StyleSpan::class.java)
                    if (boldStyleSpan.isEmpty()) {
                        setBold(null)
                    } else {
                        val noSpans = getOutSidePosition(getSpansPosition(boldStyleSpan))
                        noSpans.forEach {
                            setBold(null, it.first, it.second)
                        }
                        boldStyleSpan.forEach {
                            setBold(it)
                        }
                    }
                }
            }

            FORMAT_ITALIC -> {
                isItalicEnable = !isItalicEnable
                if (selectionStart != selectionEnd) {
                    val italicStyleSpan =
                        getSpans(selectionStart, selectionEnd, StyleSpan::class.java)
                    if (italicStyleSpan.isEmpty()) {
                        setItalic(null)
                    } else {
                        val noSpans = getOutSidePosition(getSpansPosition(italicStyleSpan))
                        noSpans.forEach {
                            setItalic(null, it.first, it.second)
                        }
                        italicStyleSpan.forEach {
                            setItalic(it)
                        }
                    }
                }
            }

            FORMAT_UNDERLINE -> {
                if (hasFormat(selectionStart, selectionEnd, UnderlineSpan::class.java)) {
                    removeSpan(selectionStart, selectionEnd, UnderlineSpan::class.java)
                } else {
                    spannableStringBuilder.setSpan(
                        UnderlineSpan(),
                        selectionStart,
                        selectionEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            FORMAT_STRIKETHROUGH -> {
                if (hasFormat(selectionStart, selectionEnd, StrikethroughSpan::class.java)) {
                    removeSpan(selectionStart, selectionEnd, StrikethroughSpan::class.java)
                } else {
                    spannableStringBuilder.setSpan(
                        StrikethroughSpan(),
                        selectionStart,
                        selectionEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            FORMAT_FONT_SIZE -> {
                fontSize = if (value is Int) {
                    value.toFloat()
                } else if (value is Float) {
                    value.toFloat()
                } else if (value is Double) {
                    value.toFloat()
                } else {
                    textSize
                }
                if (hasFormat(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)) {
                    removeSpan(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)
                }
                if (value != null) {
                    spannableStringBuilder.setSpan(
                        AbsoluteSizeSpan(fontSize.toInt()),
                        selectionStart,
                        selectionEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            FORMAT_FONT_COLOR -> {
                fontColor = if (value is String) {
                    Color.parseColor(value)
                } else {
                    value as Int
                }
                if (hasFormat(selectionStart, selectionEnd, ForegroundColorSpan::class.java)) {
                    removeSpan(selectionStart, selectionEnd, ForegroundColorSpan::class.java)
                }
                spannableStringBuilder.setSpan(
                    ForegroundColorSpan(
                        fontColor
                    ), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            FORMAT_ALIGN_LEFT -> {
                setTextAlignmentSpan(0)
            }

            FORMAT_ALIGN_CENTER -> {
                setTextAlignmentSpan(1)
            }

            FORMAT_ALIGN_RIGHT -> {
                setTextAlignmentSpan(2)
            }
        }
        text = spannableStringBuilder
        isStyleChange = false
        setSelection(finalSelectionStart, finalSelectionEnd)
        onTextSizeChangeListener(getSelectionLine(finalSelectionStart))
    }

    /**
     * Get spans position
     * 获取样式的坐标
     * @param spans
     */
    private fun <T : CharacterStyle> getSpansPosition(spans: MutableList<T>): MutableList<Pair<Int, Int>> {
        return mutableListOf<Pair<Int, Int>>().apply {
            spans.forEach {
                val spanStart = spannableStringBuilder.getSpanStart(it)
                val spanEnd = spannableStringBuilder.getSpanEnd(it)
                add(spanStart to spanEnd)
            }
        }
    }

    /**
     * Get out side position
     * 获取选中但是没有样式的区域
     * @param spans
     */
    private fun getOutSidePosition(spans: MutableList<Pair<Int, Int>>): MutableList<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var lastEnd = selectionStart
        for (span in spans) {
            if (span.first > lastEnd) {
                result.add(Pair(lastEnd, span.first))
            }
            lastEnd = span.second
        }
        if (lastEnd < selectionEnd) {
            result.add(Pair(lastEnd, selectionEnd))
        }
        return result
    }

    /**
     * Set bold
     * 设置加粗
     */
    private fun setBold(
        boldStyleSpan: StyleSpan?,
        start: Int = selectionStart,
        end: Int = selectionEnd
    ) {
        val spansStart = spannableStringBuilder.getSpanStart(boldStyleSpan)
        val spanEnd = spannableStringBuilder.getSpanEnd(boldStyleSpan)
        if (boldStyleSpan != null) {
            when (boldStyleSpan.style) {
                Typeface.BOLD_ITALIC -> {
                    spannableStringBuilder.removeSpan(boldStyleSpan)
                    spannableStringBuilder.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        spansStart,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                Typeface.BOLD -> {
                    spannableStringBuilder.removeSpan(boldStyleSpan)
                }

                Typeface.ITALIC -> {
                    spannableStringBuilder.removeSpan(boldStyleSpan)
                    spannableStringBuilder.setSpan(
                        StyleSpan(Typeface.BOLD_ITALIC),
                        spansStart,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        } else {
            spannableStringBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Set italic
     * 设置斜体
     */
    private fun setItalic(
        italicStyleSpan: StyleSpan?,
        start: Int = selectionStart,
        end: Int = selectionEnd
    ) {
        if (italicStyleSpan != null) {
            val spanStart = spannableStringBuilder.getSpanStart(italicStyleSpan)
            val spanEnd = spannableStringBuilder.getSpanEnd(italicStyleSpan)
            when (italicStyleSpan.style) {
                Typeface.BOLD_ITALIC -> {
                    spannableStringBuilder.removeSpan(italicStyleSpan)
                    spannableStringBuilder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        spanStart,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                Typeface.BOLD -> {
                    spannableStringBuilder.removeSpan(italicStyleSpan)
                    spannableStringBuilder.setSpan(
                        StyleSpan(Typeface.BOLD_ITALIC),
                        spanStart,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                Typeface.ITALIC -> {
                    spannableStringBuilder.removeSpan(italicStyleSpan)
                }
            }
        } else {
            spannableStringBuilder.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Set text alignment
     * 0 左对齐 1 居中 2 右对齐
     */
    private fun setTextAlignmentSpan(alignType: Int) {
        val blockPosition = getBlockPosition()
        val alignmentSpan =
            getSpan(blockPosition.first, blockPosition.second, AlignmentSpan::class.java)
        if (alignmentSpan != null) {
            spannableStringBuilder.removeSpan(alignmentSpan)
        }
        when (alignType) {
            0 -> {
                if (alignmentSpan?.alignment != Layout.Alignment.ALIGN_NORMAL) {
                    spannableStringBuilder.setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
                        blockPosition.first,
                        blockPosition.second,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            1 -> {
                if (alignmentSpan?.alignment != Layout.Alignment.ALIGN_CENTER) {
                    spannableStringBuilder.setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        blockPosition.first,
                        blockPosition.second,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            2 -> {
                if (alignmentSpan?.alignment != Layout.Alignment.ALIGN_OPPOSITE) {
                    spannableStringBuilder.setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE),
                        blockPosition.first,
                        blockPosition.second,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    /**
     * Set heading
     * 设置标题
     * @param headingValue
     */
    private fun setHeading(headingValue: Float) {
        val blockPosition = getBlockPosition()
        if (hasFormat(
                blockPosition.first, blockPosition.second, RelativeSizeSpan::class.java
            )
        ) {
            val spans: Array<RelativeSizeSpan> = spannableStringBuilder.getSpans(
                blockPosition.first, blockPosition.second, RelativeSizeSpan::class.java
            )
            for (span in spans) {
                if (span.sizeChange == headingValue) {
                    spannableStringBuilder.removeSpan(span)
                    removeSpan(
                        blockPosition.first, blockPosition.second, StyleSpan::class.java
                    )
                    break
                } else if (span.sizeChange != headingValue) {
                    spannableStringBuilder.removeSpan(span)
                    spannableStringBuilder.setSpan(
                        RelativeSizeSpan(headingValue),
                        blockPosition.first,
                        blockPosition.second,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    break
                }
            }
        } else {
            spannableStringBuilder.setSpan(
                RelativeSizeSpan(headingValue),
                blockPosition.first,
                blockPosition.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableStringBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                blockPosition.first,
                blockPosition.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Has format
     * 检查是否存在格式
     * @param start
     * @param end
     * @param formatClazz
     * @return
     */
    private fun hasFormat(start: Int, end: Int, formatClazz: Class<*>): Boolean {
        if (spannableStringBuilder.length < start) {
            return false
        }
        var hasFormat = false
        spannableStringBuilder.let {
            val spans: Array<*> = it.getSpans(
                start, end, formatClazz
            )
            for (span in spans) {
                if (span != null) {
                    if (span.javaClass == formatClazz) {
                        hasFormat = true
                        break
                    }
                }
            }
        }
        return hasFormat
    }

    /**
     * Get span
     * 获取所有样式
     * @param T
     * @param start
     * @param end
     * @param spanClass
     * @return
     */
    private inline fun <reified T : Any> getSpans(
        start: Int, end: Int, spanClass: Class<T>
    ): MutableList<T> {
        if (spannableStringBuilder.length < start) {
            return mutableListOf()
        }
        val spans = mutableListOf<T>()
        spannableStringBuilder.let {
            val allSpans: Array<*> = it.getSpans(
                start, end, spanClass
            )
            for (span in allSpans) {
                if (span != null) {
                    if (span is T) {
                        spans.add(span)
                    }
                }
            }
        }
        return spans
    }

    /**
     * Get span
     * 获取样式
     * @param T
     * @param start
     * @param end
     * @param spanClass
     * @return
     */
    private inline fun <reified T : Any> getSpan(
        start: Int, end: Int, spanClass: Class<T>
    ): T? {
        if (spannableStringBuilder.length < start) {
            return null
        }
        val spans = mutableListOf<T>()
        spannableStringBuilder.let {
            val allSpans: Array<*> = it.getSpans(
                start, end, spanClass
            )
            for (span in allSpans) {
                if (span != null) {
                    if (span is T) {
                        return span
                    }
                }
            }
        }
        return null
    }

    /**
     * Remove span
     * 移除效果
     * @param start
     * @param end
     * @param spanClass
     */
    private fun removeSpan(start: Int, end: Int, spanClass: Class<*>) {
        if (spannableStringBuilder.length < start) {
            return
        }
        spannableStringBuilder.let {
            val spans: Array<*> = it.getSpans(
                start, end, spanClass
            )
            for (span in spans) {
                if (span != null) {
                    if (span.javaClass == spanClass) {
                        it.removeSpan(span)
                        break
                    }
                }
            }
        }
    }

    /**
     * Get block position
     * 获取文本块
     * @return
     */
    private fun getBlockPosition(): Pair<Int, Int> {
        val nextNewLine = text?.indexOf('\n', selectionStart) ?: 0
        val previousNewLine = text?.lastIndexOf('\n', selectionStart - 1) ?: 0
        if (previousNewLine == -1) {
            return 0 to nextNewLine
        }
        return previousNewLine to nextNewLine
    }
}