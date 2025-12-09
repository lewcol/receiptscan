package com.github.lewcol.myapplication.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun millisToLocalDate(millis: Long) : LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}