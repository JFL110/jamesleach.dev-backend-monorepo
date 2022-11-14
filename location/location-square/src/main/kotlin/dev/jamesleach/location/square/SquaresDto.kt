package dev.jamesleach.location.square

data class SquaresDto(
    val longitudeIncrement: Double,
    val latitudeIncrement: Double,
    val squares: Set<Long>
)