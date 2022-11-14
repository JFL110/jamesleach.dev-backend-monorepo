package dev.jamesleach.location.square

import dev.jamesleach.dynamodb.DynamoTableClassSupplier
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class SquareCollectionEntity(
    @get:DynamoDbPartitionKey
    var extractId: String = "",
    var creationTimestamp: Long = 0,
    var latitudeIncrement: Double = 0.0,
    var longitudeIncrement: Double = 0.0,
    var payload: String = ""
)

@Configuration
class SquareCollectionEntityTableSupplier : DynamoTableClassSupplier {
    override fun getTable() = SquareCollectionEntity::class
}