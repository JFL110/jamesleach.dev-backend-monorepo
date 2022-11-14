package dev.jamesleach

import dev.jamesleach.dynamodb.DynamoTableClassSupplier
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class MessageEntity(
    @get:DynamoDbPartitionKey
    var id: String = "",
    var text: String = ""
)

@Component
class MessageEntityTableSupplier : DynamoTableClassSupplier {
    override fun getTable() = MessageEntity::class
}