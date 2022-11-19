package dev.jamesleach.location.map

import dev.jamesleach.location.geotools.GlobalSquaresDto
import dev.jamesleach.location.s3photo.PhotoDto
import dev.jamesleach.location.square.SquaresDto
import java.time.ZonedDateTime

data class MapLocationsDto(
    val squareCollection: List<SquaresDto>,
    val photos: List<PhotoDto>,
    val globalSquares: GlobalSquaresDto,
    val updatedTime: ZonedDateTime
)