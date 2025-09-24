package com.d121211069.infusense.data

data class ThresholdItem(
    val min: Int,
    val max: Int,
    val drip: Int,
    val user: String = "-",
    val hospital: String = "-"
)