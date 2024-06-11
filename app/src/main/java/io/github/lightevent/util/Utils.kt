package io.github.lightevent.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun timestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

