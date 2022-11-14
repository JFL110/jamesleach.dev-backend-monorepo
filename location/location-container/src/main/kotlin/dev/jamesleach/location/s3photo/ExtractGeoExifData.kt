package dev.jamesleach.location.s3photo

import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ExtractGeoExifData {

    private val log = LoggerFactory.getLogger(ExtractGeoExifData::class.java)
    private val exitDateFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    fun extractLocation(photo: S3ObjectInputStream, key: String): LocationWithTime? {

        val metaData = try {
            ImageMetadataReader.readMetadata(photo)
        } catch (e: Exception) {
            log.warn("Invalid photo $key : ${e.localizedMessage}")
            return null
        } finally {
            photo.abort()
        }

        try {
            val latitude = getTag(metaData, "GPS Latitude")?.let(this::fromMinutes)
            val longitude = getTag(metaData, "GPS Longitude")?.let(this::fromMinutes)
            val time = (getTag(metaData, "Date/Time Original") ?: getTag(metaData, "Date/Time"))
                ?.let {
                    LocalDateTime.parse(it, exitDateFormat)
                }

            if (time == null || latitude == null || longitude == null) {
                log.info("No time / latitude / longitude found for photo $key")
                return null
            }

            log.info("$key - [$time] - [$latitude],[$longitude]")
            return LocationWithTime(latitude, longitude, time)
        } catch (e: Exception) {
            log.warn("Failed to parse metadata for $key : ${e.localizedMessage}")
            return null
        }
    }

    private fun getTag(metaData: Metadata, tagName: String) =
        metaData.directories
            .flatMap { d -> d.tags }
            .filter { t -> tagName == t.tagName }
            .map { obj -> obj.description }
            .firstOrNull()

    /**
     * Convert an EXIF minutes value latitude/longitude.
     */
    private fun fromMinutes(str: String): Double? {
        val parts = str.replace("'", "")
            .replace("°", "")
            .replace("\"", "")
            .split(" ")

        if (parts.size != 3 || parts.any { it.isEmpty() }) {
            return null
        }

        val firstPart = parts[0].toDouble()
        return firstPart +
                (if (firstPart < 0) -1 else 1) * parts[1].toDouble() / 60 +
                (if (firstPart < 0) -1 else 1) * parts[2].toDouble() / 3600
    }
}