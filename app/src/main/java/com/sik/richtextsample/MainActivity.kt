package com.sik.richtextsample

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.sik.richtext.LineNumberView
import com.sik.richtext.MarkData
import com.sik.richtext.RichEditText
import com.sik.richtext.RichEditTextConvert
import com.sik.richtext.RichEditTextFormat
import java.io.File


class MainActivity : AppCompatActivity() {
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
        findViewById<TextView>(R.id.mark_point).setOnClickListener {
            lineNumberView.addDataNum(MarkData().apply {
                position = visualEditor.getCurrentMarkPosition()
            })
        }
        findViewById<TextView>(R.id.line_background).setOnClickListener {
            visualEditor.switchShowBackgroundLine()
        }
        findViewById<TextView>(R.id.title_1).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_HEADING_1)
        }
        findViewById<TextView>(R.id.title_2).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_HEADING_2)
        }
        findViewById<TextView>(R.id.bold).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_BOLD)
        }
        findViewById<TextView>(R.id.italic).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ITALIC)
        }
        findViewById<TextView>(R.id.underline).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_UNDERLINE)
        }
        findViewById<TextView>(R.id.strikethrough).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_STRIKETHROUGH)
        }
        findViewById<TextView>(R.id.font_size).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_FONT_SIZE, 150)
        }
        findViewById<TextView>(R.id.font_color).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_FONT_COLOR, Color.RED)
        }
        findViewById<TextView>(R.id.align_left).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ALIGN_LEFT)
        }
        findViewById<TextView>(R.id.align_center).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ALIGN_CENTER)
        }
        findViewById<TextView>(R.id.align_right).setOnClickListener {
            visualEditor.format(RichEditTextFormat.FORMAT_ALIGN_RIGHT)
        }
        val pageMode = findViewById<TextView>(R.id.page_mode)
        pageMode.setOnClickListener {
            if (pageMode.text == "翻页") {
                visualEditor.setPageMode(true)
                pageMode.text = "滑动"
            } else {
                visualEditor.setPageMode(false)
                pageMode.text = "翻页"
            }
        }
        findViewById<TextView>(R.id.previous).setOnClickListener {
            visualEditor.gotoPrevious()
        }
        findViewById<TextView>(R.id.next).setOnClickListener {
            visualEditor.gotoNext()
        }
        findViewById<TextView>(R.id.export).setOnClickListener {
            RichEditTextConvert.exportDocx("/sdcard/download/output.docx",visualEditor.getSpannableStringBuilder())
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
        visualEditor.bindLineNumberView(lineNumberView)
//        visualEditor.setTextData(FileIOUtils.readFile2String("/sdcard/download/output.html"))
    }

}