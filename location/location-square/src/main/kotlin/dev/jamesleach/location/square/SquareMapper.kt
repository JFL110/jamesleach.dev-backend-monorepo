package dev.jamesleach.location.square

import org.springframework.stereotype.Component
import kotlin.math.floor

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
        val y = ((latitude + 85) / longitudeIncrement).toLong()
        val x = ((longitude + 180) / latitudeIncrement).toLong()
        return (x + y * gridWidth).toLong()
    }

    fun toBoundingBox(square: Long): BoundingBox {
        val x = square % gridWidth
        val y = floor(square / gridWidth)
        val eastLatitude = y * latitudeIncrement - 85
        val southLongitude = x * longitudeIncrement - 180
        return BoundingBox(
            northWest =  Point(
                latitude = eastLatitude + latitudeIncrement,
                longitude = southLongitude + longitudeIncrement
            ),
            southEast =  Point(
                latitude = eastLatitude,
                longitude = southLongitude
            )
        )
    }
}