package dev.jamesleach.location.s3photo

import java.time.LocalDateTime

data class LocationWithTime(
    val latitude: Double,
    val longitude: Double,
    val time: LocalDateTime
)