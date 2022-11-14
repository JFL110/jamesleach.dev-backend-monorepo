package dev.jamesleach.location.s3photo

import java.time.LocalDateTime

data class PhotoDto(
    val latitude: Double,
    val longitude: Double,
    val time: LocalDateTime,
    val url: String,
)
