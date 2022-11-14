package dev.jamesleach.location.googletakeout

import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.location.square.SquareCollection
import dev.jamesleach.location.square.SquareCollectionDao
import dev.jamesleach.location.square.SquareMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream

private val log = LoggerFactory.getLogger(TakeoutExtractService::class.java)

@Component
class TakeoutExtractService(
    private val squareMapper: SquareMapper,
    private val zipStreamJsonReader: ZipStreamJsonReader,
    private val jsonConsumers: List<JsonConsumer>,
    private val squareCollectionDao: SquareCollectionDao,
    private val nowSupplier: ZonedNowSupplier
) {

    fun extract(extractName: String, inputStream: InputStream) {
        log.info("Starting extract '$extractName'")

        val squares = zipStreamJsonReader.processJsonInZip(inputStream, jsonConsumers)
            .map {
                squareMapper.toSquare(it.latitude, it.longitude)
            }.toSet()

        log.info("Saving extract '$extractName'")
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