package dev.jamesleach.location.map

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.RandomGuidSupplier
import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.location.geotools.GlobalSquaresService
import dev.jamesleach.location.owntracks.LocationHistoryDao
import dev.jamesleach.location.s3photo.S3PhotoProcessor
import dev.jamesleach.location.square.SquareCollectionDao
import dev.jamesleach.location.square.SquareMapper
import dev.jamesleach.location.square.SquaresDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

private val log = LoggerFactory.getLogger(MapLocationDigester::class.java)

@Component
class MapLocationDigester(
    private val squareCollectionDao: SquareCollectionDao,
    private val objectMapper: ObjectMapper,
    private val s3Client: AmazonS3,
    private val s3PhotoProcessor: S3PhotoProcessor,
    private val nowSupplier: ZonedNowSupplier,
    private val squareMapper: SquareMapper,
    private val globalSquaresService: GlobalSquaresService,
    private val locationHistoryDao: LocationHistoryDao,
    private val randomGuidSupplier: RandomGuidSupplier,
    @Value("\${s3.locations.bucketName}") val bucketName: String,
    @Value("\${s3.locations.fileName}") val fileName: String,
) {
    fun digestLocations() {
        log.info("Reading square collections from DynamoDB")
        val squareCollections = squareCollectionDao.scan()

        log.info("Reading photos squares from S3")
        val photos = s3PhotoProcessor.listPhotos()

        log.info("Reading location history squares")
        val locationHistorySquares =
            locationHistoryDao.scan().map { squareMapper.toSquare(it.latitude, it.longitude) }.toSet()

        log.info("Aggregating squares")
        val allSquaresByIncrements = squareCollections
            .groupBy { IncrementsKey(it.latitudeIncrement, it.longitudeIncrement) }
            .mapValues { it.value.flatMap { it.squares }.toSet() }
            .map {
                SquaresDto(
                    it.key.latitudeIncrement,
                    it.key.longitudeIncrement,
                    it.value
                )
            } + SquaresDto(
            squareMapper.longitudeIncrement,
            squareMapper.latitudeIncrement,
            locationHistorySquares
        ) + SquaresDto(
            squareMapper.longitudeIncrement,
            squareMapper.latitudeIncrement,
            photos.map { squareMapper.toSquare(it.latitude, it.longitude) }.toSet()
        )

        log.info("Calculating global square statistics")
        val globalSquares = globalSquaresService.buildGlobalSquaresDto(
            allSquaresByIncrements
                .filter {
                    it.latitudeIncrement == squareMapper.latitudeIncrement
                            && it.longitudeIncrement == squareMapper.longitudeIncrement
                }
                .flatMap { it.squares }
                .toSet()
        )

        log.info("Saving location digest to S3")
        val output = MapLocationsDto(
            allSquaresByIncrements,
            photos,
            globalSquares,
            nowSupplier.get()
        )
        putToS3(objectMapper.writeValueAsString(output))

        val locationHistoryConsolationGroup = randomGuidSupplier.get()
        log.info("Consolidating pending location histories into group $locationHistoryConsolationGroup")
        locationHistoryDao.consolidatePending(locationHistoryConsolationGroup)
    }

    private fun putToS3(json: String) {
        val metadata = ObjectMetadata()
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        metadata.contentType = "application/json"
        metadata.contentLength = bytes.size.toLong()
        val request = com.amazonaws.services.s3.model.PutObjectRequest(
            bucketName,
            fileName,
            ByteArrayInputStream(bytes),
            metadata
        )
        request.cannedAcl = CannedAccessControlList.PublicRead
        s3Client.putObject(request)
    }

    private data class IncrementsKey(
        val latitudeIncrement: Double,
        val longitudeIncrement: Double,
    )
}