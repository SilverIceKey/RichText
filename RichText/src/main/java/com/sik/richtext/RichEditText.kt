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
import android.text.StaticLayout
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
import android.util.TypedValue
import android.view.ViewTreeObserver
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
    companion object {
        /**
         * Default Font Color
         * 默认文字颜色
         */
        val DEFAULT_FONT_COLOR = Color.parseColor("#414141")

        /**
         * Default Max Line Count
         * 默认最大行号
         */
        const val DEFAULT_MAX_LINE_COUNT = 44

        /**
         * Default Text Size
         * 默认字体大小
         */
        const val DEFAULT_TEXT_SIZE = 12f

        /**
         * Auto save time
         * 自动保存时间间隔
         */
        var autoSaveTime: Long = 5 * 1000
    }

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
    private var showBackgroundLine: Boolean = true

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
    private var fontColor: Int = DEFAULT_FONT_COLOR

    /**
     * Font size
     * 字体大小
     */
    private var fontSize: Float = DEFAULT_TEXT_SIZE

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
    private var isStyleChange: Boolean = false

    /**
     * Is check page set
     * 是否为检查页面设置
     */
    private var isCheckPageSet = false

    /**
     * Is turn page set
     * 翻页设置
     */
    private var isTurnPageSet = false

    /**
     * Last check page time
     * 最后检查空白页的时间
     */
    private var lastCheckPageTime: Long = 0

    /**
     * Max line count
     * 最大行数
     */
    private val maxLineCount: Int = DEFAULT_MAX_LINE_COUNT

    /**
     * Is page mode
     * 是否翻页模式
     */
    private var isPageMode: Boolean = false

    /**
     * Current page
     * 当前页码
     */
    private var currentPage: Int = 0

    /**
     * Page count
     * 页码和文字数
     */
    private var pageCount: MutableMap<Int, Int> = mutableMapOf()

    /**
     * Page start index
     * 起始文字数
     */
    private var pageStartIndex: Int = 0

    /**
     * Line number view
     * 行号控件
     */
    private var lineNumberView: LineNumberView? = null

    /**
     * Cursor position
     * 光标位置
     */
    private var cursorPosition: Int = 0

    /**
     * Text watcher
     * 文本修改监听
     */
    private lateinit var textWatcher: TextWatcher

    /**
     * Save listener
     * 保存监听
     */
    private var autoSaveListener: (String) -> Unit = {}

    init {
        mBackgroundLinePaint.style = Paint.Style.STROKE
        mBackgroundLinePaint.color = Color.BLACK  // 设置线的颜色

        background = null
        setTextData("")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, DEFAULT_TEXT_SIZE)
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isCheckPageSet) {
                    cursorPosition = selectionStart
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                startInput = start + pageStartIndex
                countInput = count
                if (!isCheckPageSet) {
                    cursorPosition += count - before
                }
                if (!isTurnPageSet || !isCheckPageSet) {
                    if (before > count) { // 删除操作
                        val deleteStart = start + pageStartIndex
                        val deleteEnd = start + before + pageStartIndex
                        spannableStringBuilder.delete(deleteStart, deleteEnd)
                    } else if (count > before) { // 添加操作
                        val insertStart = start + pageStartIndex
                        val newText = s!!.subSequence(start, start + count)
                        spannableStringBuilder.replace(insertStart, insertStart + before, newText)
                    }
                    lineNumberView?.markChangeCheck(
                        start, before, count
                    )
                }
            }

            override fun afterTextChanged(s: Editable) {
                if (!isStyleChange) {
                    isStyleChange = true
                    checkAndSetSpan()
                    isTurnPageSet = true
                    isCheckPageSet = true
                    if (isPageMode) {
                        isStyleChange = true
                        text = spannableStringBuilder.subSequence(
                            pageStartIndex,
                            layout.getLineEnd(getLastLine()) + pageStartIndex
                        ) as SpannableStringBuilder
                        isTurnPageSet = false
                        isCheckPageSet = false
                        checkPage()
                    } else {
                        text = spannableStringBuilder
                        isTurnPageSet = false
                        isCheckPageSet = false
                        checkPage()
                    }
                    if (isPageMode) {
                        val subStringText = spannableStringBuilder.subSequence(
                            layout.getLineStart(maxLineCount - 1),
                            layout.getLineEnd(maxLineCount - 1)
                        )
                        val staticLayout = StaticLayout(
                            subStringText,
                            paint,
                            width,
                            Layout.Alignment.ALIGN_NORMAL,
                            1f,
                            0f,
                            false
                        )
                        val lineWidth = staticLayout.getLineWidth(0) // 获取第一行的宽度
                        if (lineWidth >= (width - paddingLeft - paddingRight) * 0.93f &&
                            cursorPosition >= layout.getLineEnd(getLastLine()) - pageStartIndex
                        ) {
                            gotoNext()
                            setSelection(cursorPosition - pageStartIndex)
                        } else {
                            setSelection(cursorPosition)
                        }
                    } else {
                        setSelection(cursorPosition)
                    }
                    isStyleChange = false
                }
            }

        }
        addTextChangedListener(textWatcher)
        startAutoSave()
    }


    override fun onDraw(canvas: Canvas) {
        if (showBackgroundLine) {
            val count = if (isPageMode) maxLineCount else lineCount
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
     * Set auto save listener
     * 设置自动保存监听
     * @param autoSaveListener
     * @receiver
     */
    fun setAutoSaveListener(autoSaveListener: (String) -> Unit = {}) {
        this.autoSaveListener = autoSaveListener
    }

    /**
     * Bind line number view
     * 绑定行号控件
     * @param lineNumberView
     */
    fun bindLineNumberView(lineNumberView: LineNumberView) {
        lineNumberView.bindEditText(this)
        this.lineNumberView = lineNumberView
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

    /**
     * Set page mode
     * 设置翻页模式
     * @param isPageMode
     */
    fun setPageMode(isPageMode: Boolean) {
        this.isPageMode = isPageMode
        if (isPageMode) {
            currentPage = 0
            pageStartIndex = 0
            pageCount.clear()
            isStyleChange = true
            isCheckPageSet = true
            isTurnPageSet = true
            text = spannableStringBuilder.subSequence(
                layout.getLineStart(0) + pageStartIndex,
                layout.getLineEnd(getLastLine())
            ) as SpannableStringBuilder
            lineNumberView?.updateLineNumbers()
            lineNumberView?.dataNumSet()
            lineNumberView?.postInvalidate()
            isStyleChange = false
            isCheckPageSet = false
            isTurnPageSet = false
        } else {
            currentPage = 0
            pageStartIndex = 0
            pageCount.clear()
            isStyleChange = true
            isCheckPageSet = true
            isTurnPageSet = true
            text = spannableStringBuilder
            lineNumberView?.updateLineNumbers()
            lineNumberView?.dataNumSet()
            lineNumberView?.postInvalidate()
            isStyleChange = false
            isCheckPageSet = false
            isTurnPageSet = false
        }
    }

    /**
     * Is page mode
     * 是否为翻页模式
     * @return
     */
    fun isPageMode(): Boolean {
        return isPageMode
    }

    /**
     * Goto next
     * 下一页
     */
    fun gotoNext() {
        if (!isPageMode) {
            return
        }
        if (isBlank()) {
            return
        }
        pageStartIndex += layout.getLineEnd(getLastLine())
        pageCount[currentPage] =
            layout.getLineEnd(getLastLine())
        currentPage++
        isStyleChange = true
        isTurnPageSet = true
        isCheckPageSet = true
        checkNextPage()
        text = spannableStringBuilder.subSequence(
            pageStartIndex, spannableStringBuilder.length
        ) as SpannableStringBuilder
        text = spannableStringBuilder.subSequence(
            pageStartIndex,
            layout.getLineEnd(getLastLine()) + pageStartIndex
        ) as SpannableStringBuilder
        lineNumberView?.updateLineNumbers()
        lineNumberView?.dataNumSet()
        lineNumberView?.postInvalidate()
        isStyleChange = false
        isTurnPageSet = false
        isCheckPageSet = false
    }

    /**
     * Goto previous
     * 上一页
     */
    fun gotoPrevious() {
        if (!isPageMode) {
            return
        }
        if (currentPage == 0) {
            return
        }
        currentPage--
        pageStartIndex -= (pageCount[currentPage] ?: 0)
        isStyleChange = true
        isTurnPageSet = true
        isCheckPageSet = true
        val pageLength = (pageCount[currentPage] ?: 0)
        text = spannableStringBuilder.subSequence(
            pageStartIndex, pageStartIndex + pageLength
        ) as SpannableStringBuilder
        lineNumberView?.updateLineNumbers()
        lineNumberView?.dataNumSet()
        lineNumberView?.postInvalidate()
        pageCount[currentPage] = 0
        isStyleChange = false
        isTurnPageSet = false
        isCheckPageSet = false
    }

    /**
     * Get page start index
     * 获取页起始下标
     * @return
     */
    fun getPageStartIndex(): Int {
        return pageStartIndex
    }

    /**
     * Get current mark position
     * 获取当前打点位置
     * @return
     */
    fun getCurrentMarkPosition(): Int {
        return if (isPageMode) {
            selectionStart + pageStartIndex
        } else {
            selectionStart
        }
    }

    /**
     * Get current page position
     * 获取当前页面的标记位置范围
     * @return
     */
    fun getCurrentPagePosition(): IntArray {
        return if (isPageMode) {
            intArrayOf(
                pageStartIndex,
                layout.getLineEnd(getLastLine()) + pageStartIndex
            )
        } else {
            intArrayOf(0, spannableStringBuilder.length)
        }
    }

    /**
     * Get spannable string builder
     * 获取文本带样式
     * @return
     */
    fun getSpannableStringBuilder(): SpannableStringBuilder {
        return spannableStringBuilder
    }

    /**
     * Set text data
     * 设置文本数据
     */
    fun setTextData(html: String) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 在布局完成后执行的代码
                if (html.isEmpty()) {
                    checkPage()
                } else {
                    isStyleChange = true
                    isTurnPageSet = true
                    isCheckPageSet = true
                    spannableStringBuilder = RichEditTextConvert.convertFromHtml(html)
                    text = spannableStringBuilder
                    isStyleChange = false
                    isTurnPageSet = false
                    isCheckPageSet = false
                }
                postDelayed({
                    lineNumberView?.updateLineNumbers()
                    lineNumberView?.dataNumSet()
                    lineNumberView?.postInvalidate()
                }, 20)
                setSelection(0)
                // 移除监听器，以防止多次调用
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
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
        isTurnPageSet = true
        isCheckPageSet = true
        val finalSelectionStart = selectionStart + pageStartIndex
        val finalSelectionEnd = selectionEnd + pageStartIndex
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
                removeSpans(
                    blockPosition.first + pageStartIndex,
                    blockPosition.second + pageStartIndex,
                    StyleSpan::class.java
                )
                removeSpans(
                    blockPosition.first + pageStartIndex,
                    blockPosition.second + pageStartIndex,
                    RelativeSizeSpan::class.java
                )
            }

            FORMAT_BOLD -> {
                isBoldEnable = !isBoldEnable
                if (selectionStart != selectionEnd) {
                    val boldStyleSpan = getSpans(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        StyleSpan::class.java
                    )
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
                    val italicStyleSpan = getSpans(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        StyleSpan::class.java
                    )
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
                if (hasFormat(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        UnderlineSpan::class.java
                    )
                ) {
                    removeSpan(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        UnderlineSpan::class.java
                    )
                } else {
                    spannableStringBuilder.setSpan(
                        UnderlineSpan(),
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            FORMAT_STRIKETHROUGH -> {
                if (hasFormat(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        StrikethroughSpan::class.java
                    )
                ) {
                    removeSpan(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        StrikethroughSpan::class.java
                    )
                } else {
                    spannableStringBuilder.setSpan(
                        StrikethroughSpan(),
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
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
                if (hasFormat(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        AbsoluteSizeSpan::class.java
                    )
                ) {
                    removeSpans(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        AbsoluteSizeSpan::class.java
                    )
                }
                if (value != null) {
                    spannableStringBuilder.setSpan(
                        AbsoluteSizeSpan(fontSize.toInt()),
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
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
                if (hasFormat(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        ForegroundColorSpan::class.java
                    )
                ) {
                    removeSpans(
                        selectionStart + pageStartIndex,
                        selectionEnd + pageStartIndex,
                        ForegroundColorSpan::class.java
                    )
                }
                spannableStringBuilder.setSpan(
                    ForegroundColorSpan(
                        fontColor
                    ),
                    selectionStart + pageStartIndex,
                    selectionEnd + pageStartIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
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
        isTurnPageSet = false
        isCheckPageSet = false
        setSelection(finalSelectionStart, finalSelectionEnd)
        if (richEditTextFormat in arrayOf(
                FORMAT_HEADING_1,
                FORMAT_HEADING_2,
                FORMAT_HEADING_3,
                FORMAT_HEADING_4,
                FORMAT_HEADING_5,
                FORMAT_HEADING_REMOVE
            )
        ) {
            onTextSizeChangeListener(-1)
        } else {
            onTextSizeChangeListener(getSelectionLine(finalSelectionStart))
        }
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
        var lastEnd = selectionStart + pageStartIndex
        for (span in spans) {
            if (span.first > lastEnd) {
                result.add(Pair(lastEnd, span.first))
            }
            lastEnd = span.second
        }
        if (lastEnd < selectionEnd + pageStartIndex) {
            result.add(Pair(lastEnd, selectionEnd + pageStartIndex))
        }
        return result
    }

    /**
     * Set bold
     * 设置加粗
     */
    private fun setBold(
        boldStyleSpan: StyleSpan?,
        start: Int = selectionStart + pageStartIndex,
        end: Int = selectionEnd + pageStartIndex
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
        start: Int = selectionStart + pageStartIndex,
        end: Int = selectionEnd + pageStartIndex
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
        val alignmentSpan = getSpan(
            blockPosition.first + pageStartIndex,
            blockPosition.second + pageStartIndex,
            AlignmentSpan::class.java
        )
        if (alignmentSpan != null) {
            spannableStringBuilder.removeSpan(alignmentSpan)
        }
        when (alignType) {
            0 -> {
                if (alignmentSpan?.alignment != Layout.Alignment.ALIGN_NORMAL) {
                    spannableStringBuilder.setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
                        blockPosition.first + pageStartIndex,
                        blockPosition.second + pageStartIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            1 -> {
                if (alignmentSpan?.alignment != Layout.Alignment.ALIGN_CENTER) {
                    spannableStringBuilder.setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        blockPosition.first + pageStartIndex,
                        blockPosition.second + pageStartIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            2 -> {
                if (alignmentSpan?.alignment != Layout.Alignment.ALIGN_OPPOSITE) {
                    spannableStringBuilder.setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE),
                        blockPosition.first + pageStartIndex,
                        blockPosition.second + pageStartIndex,
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
        removeSpans(
            blockPosition.first + pageStartIndex,
            blockPosition.second + pageStartIndex,
            AbsoluteSizeSpan::class.java
        )
        removeSpans(
            blockPosition.first + pageStartIndex,
            blockPosition.second + pageStartIndex,
            StyleSpan::class.java
        )
        if (hasFormat(
                blockPosition.first + pageStartIndex,
                blockPosition.second + pageStartIndex,
                RelativeSizeSpan::class.java
            )
        ) {
            val spans: Array<RelativeSizeSpan> = spannableStringBuilder.getSpans(
                blockPosition.first + pageStartIndex,
                blockPosition.second + pageStartIndex,
                RelativeSizeSpan::class.java
            )
            for (span in spans) {
                if (span.sizeChange == headingValue) {
                    spannableStringBuilder.removeSpan(span)
                    removeSpans(
                        blockPosition.first + pageStartIndex,
                        blockPosition.second + pageStartIndex,
                        RelativeSizeSpan::class.java
                    )
                    break
                } else if (span.sizeChange != headingValue) {
                    spannableStringBuilder.removeSpan(span)
                    spannableStringBuilder.setSpan(
                        RelativeSizeSpan(headingValue),
                        blockPosition.first + pageStartIndex,
                        blockPosition.second + pageStartIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    break
                }
            }
        } else {
            spannableStringBuilder.setSpan(
                RelativeSizeSpan(headingValue),
                blockPosition.first + pageStartIndex,
                blockPosition.second + pageStartIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableStringBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                blockPosition.first + pageStartIndex,
                blockPosition.second + pageStartIndex,
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
     * Remove spans
     * 移除所有效果
     * @param start
     * @param end
     * @param spanClass
     */
    private fun removeSpans(start: Int, end: Int, spanClass: Class<*>) {
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

    /**
     * Check next page
     * 检查下一页是否存在足够数据
     */
    private fun checkNextPage() {
        val takeLast = spannableStringBuilder.subSequence(
            pageStartIndex - (pageCount[currentPage] ?: 0),
            pageStartIndex
        ).split("\n").takeLast(maxLineCount)
        if (takeLast.size < maxLineCount) {
            for (i in 0 until maxLineCount - takeLast.size) {
                spannableStringBuilder.append("\n")
            }
        }
    }

    /**
     * Check page
     * 检查空白页
     */
    private fun checkPage() {
        if (System.currentTimeMillis() - lastCheckPageTime < 300) {
            lastCheckPageTime = System.currentTimeMillis()
            return
        }
        if (isCheckPageSet) {
            return
        }
        lastCheckPageTime = System.currentTimeMillis()
        val lastMaxLineCount =
            spannableStringBuilder.subSequence(pageStartIndex, spannableStringBuilder.length)
                .split("\n").takeLast(maxLineCount)
        if (lastMaxLineCount.size < maxLineCount) {
            isStyleChange = true
            isCheckPageSet = true
            for (i in 0 until maxLineCount - lastMaxLineCount.size) {
                append("\n")
            }
            onTextSizeChangeListener(-1)
            isStyleChange = false
            isCheckPageSet = false
        } else {
            if (!lastMaxLineCount.all { it.isEmpty() } && !isPageMode) {
                isStyleChange = true
                isCheckPageSet = true
                for (i in 0 until maxLineCount) {
                    append("\n")
                }
                onTextSizeChangeListener(-1)
                isStyleChange = false
                isCheckPageSet = false
            }
        }
    }

    /**
     * Start auto save
     * 开启自动保存
     */
    private fun startAutoSave() {
        autoSaveListener(RichEditTextConvert.convertToHtml(spannableStringBuilder))
        postDelayed({ startAutoSave() }, autoSaveTime)
    }

    /**
     * Check and set span
     * 检查并设置样式
     * @param start
     */
    private fun checkAndSetSpan() {
        if (!hasFormat(startInput, startInput + countInput, ForegroundColorSpan::class.java)) {
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
                    ), startInput, startInput + countInput, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        if (!hasFormat(startInput, startInput + countInput, AbsoluteSizeSpan::class.java)) {
            spannableStringBuilder.setSpan(
                AbsoluteSizeSpan(
                    fontSize.toInt()
                ), startInput, startInput + countInput, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (!hasFormat(startInput, startInput + countInput, ForegroundColorSpan::class.java)) {
            spannableStringBuilder.setSpan(
                ForegroundColorSpan(
                    fontColor
                ), startInput, startInput + countInput, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Get last line
     * 获取最后一行的行号
     * @return
     */
    private fun getLastLine(): Int {
        return maxLineCount - 1
    }

    /**
     * Is blank
     * 是否为空白页
     */
    private fun isBlank(): Boolean {
        val lastMaxLineCount = text?.split("\n")?.takeLast(maxLineCount - 1) ?: arrayListOf()
        return lastMaxLineCount.all { it.isEmpty() }
    }

    override fun setTextSize(unit: Int, size: Float) {
        val pixels = size * (resources.displayMetrics.densityDpi / 72f)
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixels)
        onTextSizeChangeListener(-1)
    }
}