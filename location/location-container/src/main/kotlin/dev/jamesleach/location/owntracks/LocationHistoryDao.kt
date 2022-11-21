package dev.jamesleach.location.owntracks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.jamesleach.ZonedNowSupplier
import dev.jamesleach.dynamodb.DynamoTableNameResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

const val PENDING_GROUP_ID = "pending"

@Component
class LocationHistoryDao(
    private val objectMapper: ObjectMapper,
    private val nowSupplier: ZonedNowSupplier,
    private val enhancedClient: DynamoDbEnhancedClient,
    private val dynamoTableNameResolver: DynamoTableNameResolver,
    @Value("\${digest.owntracks.consolidateSize}") val consolidateSize: Int,
) {

    private val table by lazy {
        enhancedClient.table(
            dynamoTableNameResolver.getName(LocationHistoryEntity::class),
            TableSchema.fromClass(LocationHistoryEntity::class.java)
        )
    }

    fun createSinglePending(locationUpdate: LocationUpdate) {
        table.putItem(
            LocationHistoryEntity(
                PENDING_GROUP_ID,
                nowSupplier.get().toInstant().toEpochMilli(),
                objectMapper.writeValueAsString(listOf(locationUpdate))
            )
        )
    }

    fun scan() = table.scan().items().flatMap {
        objectMapper.readValue<List<LocationUpdate>>(it.payload)
    }

    fun consolidatePending(groupId: String) {
        val pending = listPending()

        if (pending.size < consolidateSize) {
            return
        }

        val locations = pending.flatMap {
            objectMapper.readValue<List<LocationUpdate>>(
                it.payload
            )
        }

        table.putItem(
            LocationHistoryEntity(
                groupId,
                locations.maxOf { it.time },
                objectMapper.writeValueAsString(locations)
            )
        )

        pending.forEach(table::deleteItem)
    }

    private fun listPending() =
        table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(PENDING_GROUP_ID).build()))
            .items()
            .toList()

    /**
     * For test verifications only
     */
    internal fun itemsCount() = table.scan().items().count()
}