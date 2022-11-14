package dev.jamesleach

import dev.jamesleach.dynamodb.DefaultDynamoTableNameResolver
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

@Component
class MessageDao(
    enhancedClient: DynamoDbEnhancedClient,
    dynamoTableNameResolver: DefaultDynamoTableNameResolver
) {

    val table: DynamoDbTable<MessageEntity> by lazy {
        enhancedClient.table(dynamoTableNameResolver.getName(MessageEntity::class), TableSchema.fromClass(MessageEntity::class.java))
    }

    fun create(id: String, text: String) {
        table.putItem(MessageEntity(id, text))
    }

    fun get(id: String) = table.getItem(Key.builder().partitionValue(id).build())?.text
}