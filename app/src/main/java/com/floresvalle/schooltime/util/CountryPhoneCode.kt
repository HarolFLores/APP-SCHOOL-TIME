package com.floresvalle.schooltime.util

data class CountryPhoneCode(
    val code: String,
    val name: String,
    val dialCode: String,
    val flag: String,
    val expectedLength: Int,
    val placeholder: String
)
