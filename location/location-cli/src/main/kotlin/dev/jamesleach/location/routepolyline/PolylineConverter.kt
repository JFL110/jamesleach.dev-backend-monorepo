package dev.jamesleach.location.routepolyline

import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.location.square.SquareCollection
import dev.jamesleach.location.square.SquareCollectionDao
import dev.jamesleach.location.square.SquareMapper
import io.leonard.PolylineUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

private val log = LoggerFactory.getLogger(PolylineConverter::class.java)

@Component
class PolylineConverter(
    private val squareMapper: SquareMapper,
    private val squareCollectionDao: SquareCollectionDao,
    private val nowSupplier: ZonedNowSupplier
) {

    fun convert(extractName: String, polylineFile: Path) {
        log.info("Starting polyline conversion '$extractName'")
        val contents = Files.readString(polylineFile)
        val points = try {
            PolylineUtils.decode(contents, 5)
        } catch (_: StringIndexOutOfBoundsException) {
            PolylineUtils.decode(contents + "@", 5)
        }

        val squares = points
            .map {
                squareMapper.toSquare(it.latitude, it.longitude)
            }.toSet()

        log.info("Saving polyline conversion '$extractName'")
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