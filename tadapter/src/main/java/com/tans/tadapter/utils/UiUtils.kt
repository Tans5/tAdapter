package com.tans.tadapter.utils

import android.content.Context
import android.util.TypedValue


fun Context.dp2px(dpSize: Int): Int = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpSize.toFloat(), resources.displayMetrics) + 0.5f).toInt()

fun Context.px2dp(pxSize: Int): Int = (pxSize / resources.displayMetrics.density + 0.5f).toInt()