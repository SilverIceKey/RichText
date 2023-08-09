package com.sik.richtext

import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream


/**
 * Rich edit text convert
 * 富文本编辑器转换器
 * @constructor Create empty Rich edit text convert
 */
object RichEditTextConvert {
    /**
     * Convert to html
     * 转为Html
     * @param spannable
     * @return
     */
    fun convertToHtml(spannable: SpannableStringBuilder): String {
        val html = StringBuilder()

        var i = 0
        while (i < spannable.length) {
            val alignmentSpans = spannable.getSpans(i, i + 1, AlignmentSpan::class.java)
            val nextSpanStart =
                spannable.nextSpanTransition(i, spannable.length, AlignmentSpan::class.java)

            if (alignmentSpans.isNotEmpty()) {
                val alignment = when (alignmentSpans[0].alignment) {
                    Layout.Alignment.ALIGN_NORMAL -> "left"
                    Layout.Alignment.ALIGN_CENTER -> "center"
                    Layout.Alignment.ALIGN_OPPOSITE -> "right"
                    else -> "left"
                }
                html.append("<div style=\"text-align:$alignment;\">")
                html.append(
                    parseBlockStyles(
                        spannable.subSequence(
                            i,
                            nextSpanStart
                        ) as SpannableStringBuilder
                    )
                )
                html.append("</div>")
            } else {
                html.append(
                    parseBlockStyles(
                        spannable.subSequence(
                            i,
                            nextSpanStart
                        ) as SpannableStringBuilder
                    )
                )
            }

            i = nextSpanStart
        }

        return html.toString()
    }

    private fun parseBlockStyles(spannable: SpannableStringBuilder): String {
        val html = StringBuilder()

        var i = 0
        while (i < spannable.length) {
            // 解析标题
            val relativeSizeSpans = spannable.getSpans(i, i + 1, RelativeSizeSpan::class.java)
            val nextSpanStart =
                spannable.nextSpanTransition(i, spannable.length, RelativeSizeSpan::class.java)

            if (relativeSizeSpans.isNotEmpty()) {
                val size = relativeSizeSpans[0].sizeChange
                val tag = when (size) {
                    1.5f -> "h1"
                    1.4f -> "h2"
                    1.3f -> "h3"
                    1.2f -> "h4"
                    1.1f -> "h5"
                    // 其他标题大小
                    else -> "p"
                }
                html.append("<$tag>")
                html.append(
                    parseTextStyles(
                        spannable.subSequence(
                            i,
                            nextSpanStart
                        ) as SpannableStringBuilder
                    )
                )
                html.append("</$tag>")
            } else {
                html.append(
                    parseTextStyles(
                        spannable.subSequence(
                            i,
                            nextSpanStart
                        ) as SpannableStringBuilder
                    )
                )
            }

            i = nextSpanStart
        }

        return html.toString()
    }

    private fun parseTextStyles(spannable: SpannableStringBuilder): String {
        val html = StringBuilder()

        var i = 0
        while (i < spannable.length) {
            val spans = spannable.getSpans(i, i + 1, Any::class.java)
            val openingTags = mutableListOf<String>()
            val closingTags = mutableListOf<String>()

            // 处理样式
            for (span in spans) {
                when (span) {
                    is StyleSpan -> {
                        when (span.style) {
                            Typeface.BOLD -> {
                                openingTags.add("<b>")
                                closingTags.add("</b>")
                            }

                            Typeface.ITALIC -> {
                                openingTags.add("<i>")
                                closingTags.add("</i>")
                            }

                            Typeface.BOLD_ITALIC -> {
                                openingTags.add("<b><i>")
                                closingTags.add("</i></b>")
                            }
                        }
                    }

                    is UnderlineSpan -> {
                        openingTags.add("<u>")
                        closingTags.add("</u>")
                    }

                    is StrikethroughSpan -> {
                        openingTags.add("<del>")
                        closingTags.add("</del>")
                    }

                    is AbsoluteSizeSpan -> {
                        openingTags.add(
                            "<font font-size=\"${
                                span.size
                            }px\">"
                        )
                        closingTags.add("</font>")
                    }

                    is ForegroundColorSpan -> {
                        openingTags.add(
                            "<font color=\"${
                                String.format(
                                    "#%06X",
                                    0xFFFFFF and span.foregroundColor
                                )
                            }\">"
                        )
                        closingTags.add("</font>")
                    }
                }
            }

            val nextSpanStart = spannable.nextSpanTransition(i, spannable.length, Any::class.java)
            html.append(openingTags.joinToString(""))
            val content = spannable.subSequence(i, nextSpanStart).toString().replace("\n", "<br>")
            html.append(content)
            html.append(closingTags.reversed().joinToString(""))

            i = nextSpanStart
        }

        return html.toString()
    }

    /**
     * Convert from html
     * html转显示
     * @param html
     * @return
     */
    fun convertFromHtml(html: String): SpannableStringBuilder {
        val document = Jsoup.parse(html)
        val spannable = SpannableStringBuilder()

        for (element in document.body().children()) {
            when (element.tagName()) {
                "div" -> {
                    val alignment =
                        element.attr("style").substringAfter("text-align:").substringBefore(";")
                    val alignmentSpan = when (alignment) {
                        "center" -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)
                        "right" -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE)
                        else -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL)
                    }
                    spannable.append(parseElement(element))
                    spannable.setSpan(
                        alignmentSpan,
                        spannable.length - element.text().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                "h1" -> {
                    spannable.append(parseElement(element))
                    spannable.setSpan(
                        RelativeSizeSpan(1.5f),
                        spannable.length - element.text().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                "h2" -> {
                    spannable.append(parseElement(element))
                    spannable.setSpan(
                        RelativeSizeSpan(1.4f),
                        spannable.length - element.text().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                "h3" -> {
                    spannable.append(parseElement(element))
                    spannable.setSpan(
                        RelativeSizeSpan(1.3f),
                        spannable.length - element.text().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                "h4" -> {
                    spannable.append(parseElement(element))
                    spannable.setSpan(
                        RelativeSizeSpan(1.2f),
                        spannable.length - element.text().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                "h5" -> {
                    spannable.append(parseElement(element))
                    spannable.setSpan(
                        RelativeSizeSpan(1.1f),
                        spannable.length - element.text().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                "br" -> {
                    spannable.append("\n")
                }

                else -> spannable.append(parseElement(element))
            }
            if (element.children().isNotEmpty()){
                when (element.children()[0].tagName()) {
                    "h1" -> {
                        spannable.setSpan(
                            RelativeSizeSpan(1.5f),
                            spannable.length - element.text().length,
                            spannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    "h2" -> {
                        spannable.setSpan(
                            RelativeSizeSpan(1.4f),
                            spannable.length - element.text().length,
                            spannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    "h3" -> {
                        spannable.setSpan(
                            RelativeSizeSpan(1.3f),
                            spannable.length - element.text().length,
                            spannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    "h4" -> {
                        spannable.setSpan(
                            RelativeSizeSpan(1.2f),
                            spannable.length - element.text().length,
                            spannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    "h5" -> {
                        spannable.setSpan(
                            RelativeSizeSpan(1.1f),
                            spannable.length - element.text().length,
                            spannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }
        return spannable
    }

    private fun parseElement(element: Element): SpannableStringBuilder {
        val spannable = SpannableStringBuilder()
        val text = element.text()
        if (element.children().isNotEmpty()) {
            for (child in element.children()) {
                spannable.append(parseElement(child))
            }
        } else {
            spannable.append(text)
        }

        when (element.tagName()) {
            "b" -> spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            "i" -> spannable.setSpan(
                StyleSpan(Typeface.ITALIC),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            "u" -> spannable.setSpan(
                UnderlineSpan(),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            "del" -> spannable.setSpan(
                StrikethroughSpan(),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            "br" -> {
                spannable.append("\n")
            }

            "font" -> {
                val color = element.attr("color")
                if (color.isNotEmpty()) {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor(color)),
                        0,
                        text.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                val fontSize = element.attr("font-size").replace("px", "").toIntOrNull()
                if (fontSize != null) {
                    spannable.setSpan(
                        AbsoluteSizeSpan(fontSize),
                        0,
                        text.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            // 其他标题大小
        }

        return spannable
    }

    /**
     * Export docx
     * 导出word
     * @return
     */
    fun exportDocx(filePath: String, spannable: SpannableStringBuilder): File {
        val document = XWPFDocument()

        var paragraph = document.createParagraph()
        var run = paragraph.createRun()

        val length: Int = spannable.length
        var i = 0
        while (i < length) {
            val ch: Char = spannable[i]
            if (ch == '\n') {
                // 创建新段落以处理新行
                paragraph = document.createParagraph()
                i++
                continue
            }

            run = paragraph.createRun()
            // 找到具有相同样式的连续字符或没有样式的连续字符
            val start = i
            val end = spannable.nextSpanTransition(i, length, Any::class.java)

            // 设置段落对齐方式
            setAlignment(paragraph, spannable, start, end)

            // 设置文字内容和样式
            val text = spannable.subSequence(start, end).toString()
            run.setText(text)
            setRunStyles(run, spannable, start, end)

            i = end
        }

        // 写入文件
        val out: FileOutputStream = FileOutputStream(File(filePath))
        document.write(out)
        out.close()
        document.close()
        return File(filePath)
    }

    private fun setAlignment(
        paragraph: XWPFParagraph,
        spannable: SpannableStringBuilder,
        start: Int,
        end: Int
    ) {
        val alignmentSpans = spannable.getSpans(start, end, AlignmentSpan::class.java)
        if (alignmentSpans.isNotEmpty()) {
            val alignment = alignmentSpans[0].alignment
            if (alignment == Layout.Alignment.ALIGN_CENTER) {
                paragraph.alignment = ParagraphAlignment.CENTER
            } else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
                paragraph.alignment = ParagraphAlignment.RIGHT
            }
        }
    }

    private fun setRunStyles(run: XWPFRun, spannable: SpannableStringBuilder, start: Int, end: Int) {
        // 设置文字样式
        val styleSpans = spannable.getSpans(start, end, StyleSpan::class.java)
        val colorSpans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
        val sizeSpans = spannable.getSpans(start, end, RelativeSizeSpan::class.java)
        val underlineSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
        val strikethroughSpans = spannable.getSpans(start, end, StrikethroughSpan::class.java)

        if (styleSpans.isNotEmpty()) {
            run.isBold = styleSpans[0].style and Typeface.BOLD != 0
            run.isItalic = styleSpans[0].style and Typeface.ITALIC != 0
        }
        if (colorSpans.isNotEmpty()) {
            run.color = String.format("%06X", 0xFFFFFF and colorSpans[0].foregroundColor)
        }
        if (sizeSpans.isNotEmpty()) {
            run.fontSize = (sizeSpans[0].sizeChange * 12).toInt() // Assuming default size is 12pt
        }
        if (underlineSpans.isNotEmpty()) {
            run.underline = UnderlinePatterns.SINGLE
        }
        if (strikethroughSpans.isNotEmpty()) {
            run.isStrikeThrough = true
        }
    }


}