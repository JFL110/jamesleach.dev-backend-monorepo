package dev.jamesleach.location.gps

import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.location.square.SquareCollection
import dev.jamesleach.location.square.SquareCollectionDao
import dev.jamesleach.location.square.SquareMapper
import io.jenetics.jpx.GPX
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.streams.toList

private val log = LoggerFactory.getLogger(GpsConverter::class.java)

@Component
class GpsConverter(
    private val squareMapper: SquareMapper,
    private val squareCollectionDao: SquareCollectionDao,
    private val nowSupplier: ZonedNowSupplier
) {

    fun convert(extractName: String, gpsFile: Path) {
        log.info("Starting gps conversion '$extractName'")
        val gpx = GPX.read(gpsFile)
        val points = gpx.tracks()
            .flatMap { it.segments() }
            .flatMap { it.points() }
            .toList()

        val squares = points
            .map {
                squareMapper.toSquare(it.latitude.toDegrees(), it.longitude.toDegrees())
            }.toSet()

        log.info("Saving gps conversion '$extractName'")
        squareCollectionDao.create(
            SquareCollection(
                extractName,
                nowSupplier.get(),
                squareMapper.latitudeIncrement,
                squareMapper.longitudeIncrement,
                squares
            )
        )
    }
}