package dev.jamesleach.location

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.jamesleach.dynamodb.DynamoDbContainerSpringConfiguration
import dev.jamesleach.location.map.MapLocationsDto
import dev.jamesleach.location.s3photo.PhotoDto
import dev.jamesleach.location.square.SquareCollection
import dev.jamesleach.location.square.SquareCollectionDao
import dev.jamesleach.location.square.SquaresDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.web.reactive.server.WebTestClient
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "s3.locations.bucketName=bucket-name",
        "s3.locations.fileName=file-name",
        "s3.photo.bucketName=bucket-name",
        "s3.photo.photoFolderName=folder-name",
        "global-shapes-file=./src/main/resources/globe-shapes/ne_50m_admin_0_countries.shp"
    ]
)
@TestExecutionListeners(DynamoDbContainerSpringConfiguration::class)
@ActiveProfiles("test")
class LocationContainerIntegrationTest @Autowired constructor(
    val webTestClient: WebTestClient,
    val mockAmazonS3: AmazonS3,
    val objectMapper: ObjectMapper,
    val squareCollectionDao: SquareCollectionDao
) {
    @Test
    fun `Ping controller returns pong`() {
        webTestClient.get().uri("/ping").exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(String::class.java).isEqualTo("pong")
    }

    @Test
    fun `Digest endpoint pulls data from all sources and outputs a combined JSON file`() {
        // Given
        val bucketObjects = listOf(
            objectSummary("/file-not-in-folder.jpg", null),
            objectSummary("folder-name/folder/", null),
            objectSummary("folder-name/webp-photo.webp", null),
            objectSummary("folder-name/photo0.JPG", "./src/test/resources/test-real-photo.JPG"),
            objectSummary("folder-name/photo1.JPG", "./src/test/resources/test-nocoords.jpg"),
            objectSummary("folder-name/invalid-image.jpg", "./src/test/resources/test-invalid-image.jpg"),
            objectSummary("folder-name/photo2.JPG", "./src/test/resources/test-valid-lat-long.jpg"),
        )
        val objectListing = mock<ObjectListing>()
        `when`(objectListing.objectSummaries).thenReturn(bucketObjects)
        `when`(mockAmazonS3.listObjects("bucket-name")).thenReturn(objectListing)

        squareCollectionDao.create(
            SquareCollection(
                "extract-id-1",
                ZonedDateTime.of(2015, 11, 2, 1, 2, 4, 5, ZoneId.of("UTC")),
                0.1,
                0.1,
                setOf(1023, 1634, 1443)
            )
        )

        squareCollectionDao.create(
            SquareCollection(
                "extract-id-2",
                ZonedDateTime.of(2016, 11, 2, 1, 2, 4, 5, ZoneId.of("UTC")),
                0.1,
                0.1,
                setOf(100, 99)
            )
        )

        squareCollectionDao.create(
            SquareCollection(
                "extract-id-3",
                ZonedDateTime.of(2016, 11, 2, 1, 2, 4, 5, ZoneId.of("UTC")),
                0.2,
                0.2,
                setOf(55)
            )
        )

        // When
        webTestClient.post()
            .uri("/digest")
            .bodyValue("testing-only-key")
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(String::class.java).isEqualTo("Digest finished")

        // Then
        val putObjectRequestCaptor = argumentCaptor<PutObjectRequest>()
        verify(mockAmazonS3, times(1)).putObject(putObjectRequestCaptor.capture())

        assertEquals("bucket-name", putObjectRequestCaptor.firstValue.bucketName)
        assertEquals("file-name", putObjectRequestCaptor.firstValue.key)
        assertEquals("application/json", putObjectRequestCaptor.firstValue.metadata.contentType)

        val json = putObjectRequestCaptor.firstValue.inputStream.bufferedReader().use(BufferedReader::readText)
        assertNotNull(json)
        val mapLocationsDto = objectMapper.readValue<MapLocationsDto>(json)

        assertEquals(
            listOf(
                PhotoDto(
                    latitude = 42.77611111111111,
                    longitude = 0.6555555555555556,
                    time = LocalDateTime.of(2020, 3, 14, 15, 50, 5),
                    url = "https://bucket-name.s3.null.amazonaws.com/folder-name/photo0.JPG"
                ),
                PhotoDto(
                    latitude = 18.312805555555556,
                    longitude = 0.5273333333333334,
                    time = LocalDateTime.of(2012, 1, 1, 1, 30),
                    url = "https://bucket-name.s3.null.amazonaws.com/folder-name/photo2.JPG"
                )
            ), mapLocationsDto.photos
        )
        assertEquals(
            listOf(
                SquaresDto(
                    0.2, 0.2,
                    setOf(55)
                ),
                SquaresDto(
                    0.1, 0.1,
                    setOf(99, 100, 1023, 1634, 1443)
                ),
            ),
            mapLocationsDto.squareCollection
        )
    }

    private fun objectSummary(key: String, filePath: String?): S3ObjectSummary {
        val objectSummary = mock<S3ObjectSummary>()
        `when`(objectSummary.key).thenReturn(key)

        val s3Object = mock<S3Object>()
        val inputStream = filePath?.let { ByteArrayInputStream(Paths.get(it).toFile().readBytes()) }
        `when`(s3Object.objectContent).thenReturn(S3ObjectInputStream(inputStream, mock()))

        `when`(mockAmazonS3.getObject("bucket-name", key)).thenReturn(s3Object)
        return objectSummary
    }
}