package dev.jamesleach.location.map

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.location.s3photo.S3PhotoProcessor
import dev.jamesleach.location.square.SquareCollectionDao
import dev.jamesleach.location.square.SquaresDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

@Component
class MapLocationDigester(
    private val squareCollectionDao: SquareCollectionDao,
    private val objectMapper: ObjectMapper,
    private val s3Client: AmazonS3,
    private val s3PhotoProcessor: S3PhotoProcessor,
    private val nowSupplier: ZonedNowSupplier,
    @Value("\${s3.bucketName}") val bucketName: String,
    @Value("\${s3.fileName}") val fileName: String,
) {
    fun digestLocations() {
        val squareCollections = squareCollectionDao.scan()
        val allSquaresByIncrements = squareCollections
            .groupBy { IncrementsKey(it.latitudeIncrement, it.longitudeIncrement) }
            .mapValues { it.value.flatMap { it.squares }.toSet() }
            .map {
                SquaresDto(
                    it.key.latitudeIncrement,
                    it.key.longitudeIncrement,
                    it.value
                )
            }

        val photos = s3PhotoProcessor.listPhotos()

        val output = MapLocationsDto(
            allSquaresByIncrements,
            photos,
            nowSupplier.get()
        )

        println(objectMapper.writeValueAsString(output))
        putToS3(objectMapper.writeValueAsString(output))
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