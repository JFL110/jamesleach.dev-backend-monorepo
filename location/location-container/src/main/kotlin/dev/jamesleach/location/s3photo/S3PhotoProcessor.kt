package dev.jamesleach.location.s3photo

import com.amazonaws.services.s3.AmazonS3
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class S3PhotoProcessor(
    private val s3Client: AmazonS3,
    private val extractGeoExifData: ExtractGeoExifData,
    @Value("\${s3.photo.bucketName}") val bucketName: String,
    @Value("\${s3.photo.photoFolderName}") val photoFolderName: String,
) {
    fun listPhotos() =
        // Load all photos in the bucket
        s3Client.listObjects(bucketName)
            .objectSummaries
            .filter {
                it.key.startsWith("${photoFolderName}/")
                        && !it.key.endsWith("/")
                        && !it.key.endsWith(".webp")
            }.mapNotNull { objectSummary ->
                s3Client.getObject(bucketName, objectSummary.key)
                    .objectContent
                    .use{ extractGeoExifData.extractLocation(it, objectSummary.key) }
                    ?.let {
                        val url = "https://${bucketName}.s3.${s3Client.regionName}.amazonaws.com/${objectSummary.key}"
                        PhotoDto(
                            it.latitude,
                            it.longitude,
                            it.time,
                            url
                        )
                    }
            }
}