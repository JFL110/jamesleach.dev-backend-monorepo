package dev.jamesleach.location.square

import java.time.ZonedDateTime

data class SquareCollection(
    var extractId: String,
    var creationTime: ZonedDateTime,
    val latitudeIncrement: Double,
    val longitudeIncrement: Double,
    var squares: Set<Long>
)