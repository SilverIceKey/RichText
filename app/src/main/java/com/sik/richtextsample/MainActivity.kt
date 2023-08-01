package com.sik.richtextsample

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Html
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.html.HtmlToPdfConverter
import com.sik.richtext.LineNumberView
import com.sik.richtext.MarkData
import com.sik.richtext.RichEditText
import com.sik.richtext.RichEditTextFormat
import org.apache.commons.text.StringEscapeUtils
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private val HEADING =
            "<h1>Heading 1</h1>" + "<h2><span style=\"color:#FF0000;\">Heading</span> 2</h2>" + "<h3>Heading 3</h3>" + "<h4>Heading 4</h4>" + "<h5>Heading 5</h5>" + "<h6>Heading 6</h6>"
        private val BOLD = "<b>Bold</b><br>"
        private val ITALIC = "<i style=\"color:darkred\">Italic</i><br>"
        private val UNDERLINE = "<u style=\"color:#ff0000\">Underline</u><br>"
        private val BACKGROUND =
            "<span style=\"background-color:#005082\">BACK<b>GROUND</b></span><br>"
        private val STRIKETHROUGH =
            "<s style=\"color:#ff666666\" class=\"test\">Strikethrough</s><br>" // <s> or <strike> or <del>
        private val LINE = "<hr />"
        private val QUOTE = "<blockquote>Quote</blockquote>"
        private val LINK =
            "<a href=\"https://github.com/wordpress-mobile/WordPress-Aztec-Android\">Link</a><br>"
        private val UNKNOWN = "<iframe class=\"classic\">Menu</iframe><br>"
        private val COMMENT = "<!--Comment--><br>"
        private val COMMENT_MORE = "<!--more--><br>"
        private val COMMENT_PAGE = "<!--nextpage--><br>"
        private val HIDDEN =
            "<span></span>" + "<div class=\"first\">" + "    <div class=\"second\">" + "        <div class=\"third\">" + "            Div<br><span><b>Span</b></span><br>Hidden" + "        </div>" + "        <div class=\"fourth\"></div>" + "        <div class=\"fifth\"></div>" + "    </div>" + "    <span class=\"second last\"></span>" + "</div>" + "<br>"
        private val GUTENBERG_CODE_BLOCK =
            "<!-- wp:core/image {\"id\":316} -->\n" + "<figure class=\"wp-block-image\"><img src=\"https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/WordPress_blue_logo.svg/1200px-WordPress_blue_logo.svg.png\" alt=\"\" />\n" + "  <figcaption>The WordPress logo!</figcaption>\n" + "</figure>\n" + "<!-- /wp:core/image -->"
        private val EMOJI = "&#x1F44D;"
        private val NON_LATIN_TEXT = "测试一个"
        private val LONG_TEXT =
            "<br><br>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
        private val QUOTE_RTL = "<blockquote>לְצַטֵט<br>same quote but LTR</blockquote>"
        private val MARK =
            "<p>Donec ipsum dolor, <mark style=\"color:#ff0000\">tempor sed</mark> bibendum <mark style=\"color:#1100ff\">vita</mark>.</p>"

        private val EXAMPLE =
            HEADING + BOLD + ITALIC + UNDERLINE + STRIKETHROUGH + EMOJI + NON_LATIN_TEXT + LONG_TEXT + MARK

        private val isRunningTest: Boolean by lazy {
            false
        }
    }

    private lateinit var htmlFilePath: String
    private lateinit var lineNumberView: LineNumberView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val visualEditor = findViewById<RichEditText>(R.id.visual)
        htmlFilePath = cacheDir.absolutePath
        val htmlFile = File(htmlFilePath + File.separator + "htmlFileData.txt")
        FileUtils.createOrExistsFile(htmlFile)
        // Set HTML
        val html = FileIOUtils.readFile2String(htmlFile)
        lineNumberView = findViewById<LineNumberView>(R.id.line_number)
        lineNumberView.bindEditText(visualEditor)
        findViewById<Button>(R.id.mark_point).setOnClickListener {
            lineNumberView.addDataNum(MarkData().apply { position = visualEditor.selectionStart })
        }
        findViewById<Button>(R.id.line_background).setOnClickListener {
            visualEditor.switchShowBackgroundLine()
        }
        findViewById<Button>(R.id.title_1).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_HEADING_1)
        }
        findViewById<Button>(R.id.title_2).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_HEADING_2)
        }
        findViewById<Button>(R.id.bold).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_BOLD)
        }
        findViewById<Button>(R.id.italic).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ITALIC)
        }
        findViewById<Button>(R.id.underline).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_UNDERLINE)
        }
        findViewById<Button>(R.id.strikethrough).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_STRIKETHROUGH)
        }
        findViewById<Button>(R.id.font_size).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_FONT_SIZE, 50)
        }
        findViewById<Button>(R.id.font_color).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_FONT_COLOR, Color.RED)
        }
        findViewById<Button>(R.id.align_left).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ALIGN_LEFT)
        }
        findViewById<Button>(R.id.align_center).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ALIGN_CENTER)
        }
        findViewById<Button>(R.id.align_right).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ALIGN_RIGHT)
        }
        findViewById<Button>(R.id.previous).setOnClickListener {
        }
        findViewById<Button>(R.id.next).setOnClickListener {
            val html = Html.toHtml(visualEditor.text)
            val decodedHtml = StringEscapeUtils.unescapeHtml4(html)
            val pdfDest = File("/sdcard/download/output.pdf")
            HtmlToPdfConverter.fromHTMLString(this, "<html><body>${decodedHtml}</body></html>")
                // Configure title for the created document.
                .title("Converted document")
                // Perform the conversion.
                .convertToPdfAsync(pdfDest)
                // Subscribe to the conversion result.
                .subscribe({
                    // Open and process the converted document.
                    val document = PdfDocumentLoader.openDocument(this, Uri.fromFile(pdfDest))
                }, {
                    // Handle the error.
                })
            LogUtils.i("<html><body>${decodedHtml}</body></html>")
        }
        lineNumberView.setOnLineClickListener {
            ToastUtils.showShort("当前点击行号:${it.lineNumber}")
        }
        if (!PermissionUtils.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionUtils.permission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ).request()
        }
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri: Uri = Uri.fromParts("package", "com.sik.richtextsample", null)
            intent.data = uri
            startActivity(intent)
        }
//        visualEditor.setText(Html.fromHtml(EXAMPLE))
//        aztec = Aztec.with(visualEditor, toolbar, this).setOnImeBackListener(this)
//        if (!isRunningTest) {
//            aztec.visualEditor.enableCrashLogging(object :
//                AztecExceptionHandler.ExceptionHandlerHelper {
//                override fun shouldLog(ex: Throwable): Boolean {
//                    return true
//                }
//            })
//            aztec.visualEditor.setCalypsoMode(false)
//            aztec.addPlugin(CssUnderlinePlugin())
//            aztec.visualEditor.fromHtml(EXAMPLE)
//            println(EXAMPLE)
//        }
    }

}