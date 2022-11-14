package dev.jamesleach.location.square

import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.dynamodb.DynamoTableNameResolver

import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class SquareCollectionDao(
    private val objectMapper: ObjectMapper,
    private val enhancedClient: DynamoDbEnhancedClient,
    private val dynamoTableNameResolver: DynamoTableNameResolver
) {

    private val table by lazy {
        enhancedClient.table(
            dynamoTableNameResolver.getName(SquareCollectionEntity::class),
            TableSchema.fromClass(SquareCollectionEntity::class.java)
        )
    }

    fun create(squareCollection: SquareCollection) {
        table.putItem(
            SquareCollectionEntity(
            squareCollection.extractId,
            squareCollection.creationTime.toInstant().toEpochMilli(),
            squareCollection.latitudeIncrement,
            squareCollection.longitudeIncrement,
            objectMapper.writeValueAsString(squareCollection.squares))
        )
    }

    fun scan() = table.scan().items().map {
        SquareCollection(
            it.extractId,
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.creationTimestamp), ZoneOffset.UTC),
            it.latitudeIncrement,
            it.longitudeIncrement,
            objectMapper.readValue(it.payload),
        )
    }
}