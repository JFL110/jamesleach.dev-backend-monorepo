package dev.jamesleach.location.owntracks

import dev.jamesleach.dynamodb.DynamoTableClassSupplier
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
data class LocationHistoryEntity(
    @get:DynamoDbPartitionKey
    var groupId: String = "",
    @get:DynamoDbSortKey
    var insertTime: Long = 0,
    var payload: String = ""
)

@Configuration
class LocationHistoryEntityTableSupplier : DynamoTableClassSupplier {
    override fun getTable() = LocationHistoryEntity::class
}