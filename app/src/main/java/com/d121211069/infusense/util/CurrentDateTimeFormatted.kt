package com.d121211069.infusense.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun currentDateTimeFormatted(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy - HH.mm", Locale.getDefault())
    return sdf.format(Date())
}
