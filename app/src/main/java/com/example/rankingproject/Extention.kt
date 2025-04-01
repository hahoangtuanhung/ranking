package com.example.rankingproject

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

fun View.setSingleClick(
    clickSpendTime: Long = 500L,
    execution: () -> Unit
) {
    setOnClickListener(object : View.OnClickListener {
        var lastClickTime: Long = 0
        override fun onClick(p0: View?) {
            if (SystemClock.elapsedRealtime() - lastClickTime < clickSpendTime) {
                return
            }
            lastClickTime = SystemClock.elapsedRealtime()
            execution.invoke()
        }
    })
}

fun loadImageFromAsset(context: Context, directory: String, intoView: ImageView) {
    Glide.with(context).load(
        "file:///android_asset/$directory"
    ).into(intoView)
}

fun View?.show() {
    this?.visibility = View.VISIBLE
}

fun View?.hide() {
    this?.visibility = View.GONE
}

fun View?.invisible() {
    this?.visibility = View.INVISIBLE
}