package dev.jamesleach.location.square

data class BoundingBox(
    val northWest: Point,
    val southEast: Point,
) {

    val northEast = Point(
        latitude = northWest.latitude,
        longitude = southEast.longitude
    )

    val southWest = Point(
        latitude = southEast.latitude,
        longitude = northWest.longitude
    )
}