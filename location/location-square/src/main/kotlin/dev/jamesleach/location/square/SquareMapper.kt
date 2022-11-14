package dev.jamesleach.location.square

import org.springframework.stereotype.Component

/**
 * For the purposes of displaying on the FE map,
 *  Latitude ranges from -85 to +85
 *  Longitude ranges from -180 to +180
 */
@Component
class SquareMapper {

    final val latitudeIncrement = 0.02
    final val longitudeIncrement = 0.02
    private val gridWidth = 360 / longitudeIncrement

    fun toSquare(latitude: Double, longitude: Double): Long {
        val x = ((latitude + 85) / longitudeIncrement).toLong()
        val y = ((longitude + 180) / latitudeIncrement).toLong()
        return (x + y * gridWidth).toLong()
    }
}