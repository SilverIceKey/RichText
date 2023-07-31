package com.sik.richtext

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.marginTop

class SlideBar(context: Context, attrs: AttributeSet) :
    RelativeLayout(context, attrs) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var startClickTime: Long = 0
    private val slideImage: ImageView = ImageView(context)
    private var slideRect: Rect = Rect()
    private var onSlideChangeListener: (Float) -> Unit = {}
    private var bindingEditText: EditText? = null

    init {
        slideImage.setImageResource(R.drawable.ic_slide)
        addView(slideImage)
        visibility = GONE
    }

    /**
     * Set image resource
     * 设置指示器图片
     * @param resourceId
     */
    fun setImageResource(resourceId: Int) {
        slideImage.setImageResource(resourceId)
    }

    /**
     * Set on slide change listener
     * 设置滑动监听
     * @param onSlideChangeListener
     * @receiver
     */
    fun setOnSlideChangeListener(onSlideChangeListener: (Float) -> Unit = {}) {
        this.onSlideChangeListener = onSlideChangeListener
    }

    /**
     * Bind edit text
     * 绑定文本
     * @param editText
     */
    fun bindEditText(editText: EditText, lineNumberView: LineNumberView) {
        this.bindingEditText = editText
        editText.isVerticalScrollBarEnabled = false
        lineNumberView.addEditTextScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (isDragging) {
                return@addEditTextScrollChangeListener
            }
            if (visibility == View.GONE) {
                alpha = 1f
                visibility = View.VISIBLE
            }
            val layoutParams = slideImage.layoutParams as LayoutParams
            layoutParams.topMargin =
                (scrollY * 1f / (editText.layout.height - editText.height) * (height - slideImage.height)).toInt()
            slideImage.layoutParams = layoutParams
            slideRect =
                Rect(
                    slideImage.left,
                    slideImage.marginTop,
                    slideImage.right,
                    slideImage.marginTop + slideImage.height
                )
            handler.removeCallbacks(checkScrollRunnable)
            handler.postDelayed(checkScrollRunnable, 1200)
        }
    }

    /**
     * Check scroll runnable
     * 滚动结束隐藏
     */
    private val checkScrollRunnable = Runnable {
        if (!isDragging) {
            ValueAnimator.ofFloat(1f, 0f).apply {
                setFloatValues(1f, 0f)
                duration = 500
                addUpdateListener {
                    alpha = it.animatedValue as Float
                    if (it.animatedValue as Float == 0f) {
                        visibility = GONE
                    }
                }
                start()
            }
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w < slideImage.width) {
            val layoutParams = slideImage.layoutParams as LayoutParams
            layoutParams.width = w
            slideImage.layoutParams = layoutParams
        } else {
            val layoutParams = slideImage.layoutParams as LayoutParams
            layoutParams.leftMargin = (w - slideImage.width) / 2
            slideImage.layoutParams = layoutParams
        }
        slideRect = Rect(slideImage.left, 0, slideImage.right, slideImage.height)
        handler.postDelayed(checkScrollRunnable, 1200)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (!slideRect.contains(x.toInt(), y.toInt()) && !isDragging) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 当手指按下时，记录当前触摸点的坐标，并设置拖动标志为 true
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                isDragging = true
                startClickTime = System.currentTimeMillis()
                handler.removeCallbacks(checkScrollRunnable)
                bindingEditText?.isCursorVisible = false
                bindingEditText?.clearFocus()
            }

            MotionEvent.ACTION_MOVE -> {
                // 当手指移动时，如果拖动标志为 true，则计算偏移量并更新视图的位置
                if (isDragging) {
                    val deltaY = event.rawY - lastTouchY
                    val layoutParams = slideImage.layoutParams as LayoutParams
                    layoutParams.topMargin += deltaY.toInt()
                    if (layoutParams.topMargin < 0) {
                        layoutParams.topMargin = 0
                    } else if (layoutParams.topMargin > (height - slideImage.height)) {
                        layoutParams.topMargin = height - slideImage.height
                    }
                    slideImage.layoutParams = layoutParams
                    val percent = slideImage.marginTop * 1f / (height - slideImage.height)
                    bindingEditText?.scrollTo(0,
                        (percent * ((bindingEditText?.layout?.height?:0) - (bindingEditText?.height
                            ?: 0))).toInt()
                    )

                    onSlideChangeListener(percent)
                    // 更新上次触摸点的坐标
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 当手指抬起或者取消时，将拖动标志设置为 false
                if (System.currentTimeMillis() - startClickTime < 100) {
                    performClick()
                }
                slideRect =
                    Rect(slideImage.left, slideImage.top, slideImage.right, slideRect.bottom)
                isDragging = false
                handler.postDelayed(checkScrollRunnable, 1200)
            }
        }   // 如果您需要处理其他 onTouchEvent，请在此处调用 super.onTouchEvent(event)
        // 否则返回 true，表明我们已处理触摸事件
        return true
    }
}