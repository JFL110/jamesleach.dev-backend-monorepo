package dev.jamesleach.location.owntracks

import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.ObjectMapperConfiguration
import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.dynamodb.DefaultDynamoTableNameResolver
import dev.jamesleach.dynamodb.DynamoDbContainerExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

class LocationHistoryTest {

    companion object {
        @RegisterExtension
        @JvmField
        val dynamoContainer = DynamoDbContainerExtension(LocationHistoryEntity::class)
    }

    private val testTime = ZonedDateTime.now()
    private val objectMapper: ObjectMapper = ObjectMapperConfiguration().objectMapper()
    private val nowSupplier = mock<ZonedNowSupplier>()
    private val dynamoTableNameResolver = DefaultDynamoTableNameResolver()
    private val dao =
        LocationHistoryDao(objectMapper, nowSupplier, dynamoContainer.enhancedClient, dynamoTableNameResolver, 1)

    @Test
    fun `location updates are saved, read and consolidated`() {
        // Initially
        assertEquals(0, dao.scan().size)

        // Create
        val update1 = LocationUpdate(
            55.0,
            22.0,
            100.0,
            14.5,
            15123250
        )
        `when`(nowSupplier.get()).thenReturn(testTime)
        dao.createSinglePending(update1)

        assertEquals(1, dao.scan().size)
        assertEquals(update1, dao.scan()[0])

        val update2 = LocationUpdate(
            155.0,
            122.0,
            1100.0,
            114.5,
            16123250
        )
        `when`(nowSupplier.get()).thenReturn(testTime.plusSeconds(1))
        dao.createSinglePending(update2)

        assertEquals(2, dao.scan().size)
        assertThat(dao.scan()).containsExactlyInAnyOrder(update1, update2)
        assertEquals(2, dao.itemsCount())

        // Consolidate
        dao.consolidatePending("group-1")
        assertEquals(2, dao.scan().size)
        assertThat(dao.scan()).containsExactlyInAnyOrder(update1, update2)
        assertEquals(1, dao.itemsCount())
    }

}