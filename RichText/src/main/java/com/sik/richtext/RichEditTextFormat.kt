package com.sik.richtext

/**
 * Rich edit text format
 * 富文本格式化类型
 * @constructor Create empty Rich edit text format
 */
enum class RichEditTextFormat {
    FORMAT_HEADING_1,
    FORMAT_HEADING_2,
    FORMAT_HEADING_3,
    FORMAT_HEADING_4,
    FORMAT_HEADING_5,
    FORMAT_HEADING_REMOVE,
    FORMAT_BOLD,
    FORMAT_ITALIC,
    FORMAT_UNDERLINE,
    FORMAT_STRIKETHROUGH,
    FORMAT_FONT_SIZE,
    FORMAT_FONT_COLOR,
    FORMAT_ALIGN_LEFT,
    FORMAT_ALIGN_CENTER,
    FORMAT_ALIGN_RIGHT
}