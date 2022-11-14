package dev.jamesleach.location

import dev.jamesleach.dynamodb.DynamoDbContainerSpringConfiguration
import dev.jamesleach.location.square.SquareCollectionDao
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import java.time.Duration
import java.time.ZonedDateTime

@SpringBootTest(properties = [
    "s3.bucketName=bucket-name",
    "s3.fileName=file-name"
])
@ActiveProfiles("test")
@TestExecutionListeners(DynamoDbContainerSpringConfiguration::class)
class LocationCliIntegrationTest @Autowired constructor(
    val locationApp: LocationApp,
    val squaresCollectionDao: SquareCollectionDao
) {
    @Test
    fun testLocation() {
        // Given
        assertEquals(0, squaresCollectionDao.scan().count())

        // When
        locationApp.run("-name", "extract-name", "-path", "./src/test/resources/TestTakeout.zip")

        // Then
        val allCollections = squaresCollectionDao.scan().toList()
        assertEquals(1, allCollections.size)
        assertEquals("extract-name", allCollections[0].extractId)
        assertEquals(0.02, allCollections[0].latitudeIncrement)
        assertEquals(0.02, allCollections[0].longitudeIncrement)
        assertTrue(Duration.between(allCollections[0].creationTime, ZonedDateTime.now()).seconds < 3)

        Assertions.assertThat(allCollections[0].squares).containsExactly(161898822, 160008921, 159990921)
    }

    @Test
    fun testEmptyZip() {
        // Given
        assertEquals(0, squaresCollectionDao.scan().count())

        // When
        locationApp.run("-name", "extract-name", "-path", "./src/test/resources/EmptyZip.zip")

        // Then
        val allCollections = squaresCollectionDao.scan().toList()
        assertEquals(1, allCollections.size)
        assertEquals("extract-name", allCollections[0].extractId)
        assertEquals(0.02, allCollections[0].latitudeIncrement)
        assertEquals(0.02, allCollections[0].longitudeIncrement)
        assertTrue(Duration.between(allCollections[0].creationTime, ZonedDateTime.now()).seconds < 3)

        assertEquals(emptySet<Long>(), allCollections[0].squares)
    }
}