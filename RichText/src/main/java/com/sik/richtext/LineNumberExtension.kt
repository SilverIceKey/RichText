package com.sik.richtext

import android.widget.EditText

/**
 * Get selection line
 * 获取光标所在行号
 * @return
 */
fun RichEditText.getSelectionLine(selectPosition: Int): Int {
    return layout.getLineForOffset(selectPosition - getPageStartIndex()-1) + 1
}

/**
 * Get text without last wrap
 * 获取文本去除最后的换行
 * @return
 */
fun EditText.getTextWithoutLastWrap(): String {
    return text.replace(Regex("\\n+$"), "")
}

/**
 * Get line end
 * 获取页尾
 * @return
 */
fun EditText.getLineEnd(): Int {
    return layout.getLineEnd(layout.getLineForVertical(height))
}