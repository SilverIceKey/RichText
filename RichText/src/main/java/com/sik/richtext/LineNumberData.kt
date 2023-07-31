package com.sik.richtext

import android.graphics.Rect

class LineNumberData {
    /**
     * Line height
     * 行高
     */
    var lineHeight: Int = 0

    /**
     * Line number
     * 行号
     */
    var lineNumber: Int = 0

    /**
     * Data num
     * 当数据大于0时会在左侧添加标记
     */
    var dataNum: Int = 0

    /**
     * Click rect
     * 可点击区域
     */
    var clickRect: Rect = Rect()
}