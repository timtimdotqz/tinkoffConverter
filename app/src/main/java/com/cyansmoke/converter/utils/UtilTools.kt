package com.cyansmoke.converter.utils

import android.view.View
import androidx.core.content.ContextCompat
//немного инструментов
fun Int.getString() = TinkoffFintech.getContext().getString(this)

fun Int.getDrawable() = TinkoffFintech.getContext().getDrawable(this)

fun Int.getColor() = ContextCompat.getColor(TinkoffFintech.getContext(), this)

fun View.showIf(condition: Boolean, invisible: Boolean = false) {
    if (condition && invisible) {
        this.visibility = View.INVISIBLE
    } else if (condition) {
        this.visibility = View.GONE
    } else {
        this.visibility = View.VISIBLE
    }
}