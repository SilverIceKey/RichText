package com.sik.richtextsample

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.sik.richtext.LineNumberView
import com.sik.richtext.MarkData
import com.sik.richtext.RichText
import com.sik.richtext.SlideBar
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecExceptionHandler
import org.wordpress.aztec.AztecText
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.plugins.CssUnderlinePlugin
import org.wordpress.aztec.toolbar.AztecToolbar
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener
import org.wordpress.aztec.toolbar.ToolbarAction
import org.wordpress.aztec.toolbar.ToolbarItems
import java.io.File

class MainActivity : AppCompatActivity(), AztecText.OnImeBackListener, IAztecToolbarClickListener {
    companion object {
        private val HEADING =
            "<h1>Heading 1</h1>" + "<h2>Heading 2</h2>" + "<h3>Heading 3</h3>" + "<h4>Heading 4</h4>" + "<h5>Heading 5</h5>" + "<h6>Heading 6</h6>"
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
    protected lateinit var aztec: Aztec
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val visualEditor = findViewById<RichText>(R.id.visual)
        val toolbar = findViewById<AztecToolbar>(R.id.formatting_toolbar)
        val slideBar = findViewById<SlideBar>(R.id.slide_bar)
        htmlFilePath = cacheDir.absolutePath
        val htmlFile = File(htmlFilePath + File.separator + "htmlFileData.txt")
        FileUtils.createOrExistsFile(htmlFile)
        // Set HTML
        val html = FileIOUtils.readFile2String(htmlFile)
        lineNumberView = findViewById<LineNumberView>(R.id.line_number)
        lineNumberView.bindEditText(visualEditor)
        visualEditor.postDelayed({
            slideBar.bindEditText(visualEditor, lineNumberView)
        }, 20)
        findViewById<Button>(R.id.bold).setOnClickListener {
            lineNumberView.addDataNum(MarkData().apply { position = visualEditor.selectionStart })
        }
        findViewById<Button>(R.id.em).setOnClickListener {
            visualEditor.switchShowBackgroundLine()
        }
        findViewById<Button>(R.id.previous).setOnClickListener {
            LogUtils.i("字体大小:${visualEditor.textSize}")
            if (visualEditor.textSize > 20) {
                visualEditor.setTextSize(TypedValue.COMPLEX_UNIT_PX, visualEditor.textSize - 1)
            }

        }
        findViewById<Button>(R.id.next).setOnClickListener {
            println(visualEditor.toHtml())
        }
        lineNumberView.setOnLineClickListener {
            ToastUtils.showShort("当前点击行号:${it.lineNumber}")
        }
        toolbar.setToolbarItems(
            ToolbarItems.BasicLayout(
                ToolbarAction.HEADING,
                ToolbarAction.BACKGROUND,
                ToolbarAction.BOLD,
                ToolbarAction.ITALIC,
                ToolbarAction.UNDERLINE,
                ToolbarAction.STRIKETHROUGH,
                ToolbarAction.ALIGN_LEFT,
                ToolbarAction.ALIGN_CENTER,
                ToolbarAction.ALIGN_RIGHT,
                ToolbarItems.PLUGINS
            )
        )
        aztec = Aztec.with(visualEditor, toolbar, this).setOnImeBackListener(this)
        if (!isRunningTest) {
            aztec.visualEditor.enableCrashLogging(object :
                AztecExceptionHandler.ExceptionHandlerHelper {
                override fun shouldLog(ex: Throwable): Boolean {
                    return true
                }
            })
            aztec.visualEditor.setCalypsoMode(false)
            aztec.addPlugin(CssUnderlinePlugin())
            aztec.visualEditor.fromHtml(EXAMPLE)
            println(EXAMPLE)
        }
    }

    override fun onImeBack() {

    }

    override fun onToolbarCollapseButtonClicked() {

    }

    override fun onToolbarExpandButtonClicked() {

    }

    override fun onToolbarFormatButtonClicked(format: ITextFormat, isKeyboardShortcut: Boolean) {

    }

    override fun onToolbarHeadingButtonClicked() {

    }

    override fun onToolbarHtmlButtonClicked() {

    }

    override fun onToolbarListButtonClicked() {

    }

    override fun onToolbarMediaButtonClicked(): Boolean {
        return false
    }
}