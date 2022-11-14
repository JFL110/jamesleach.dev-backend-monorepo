package dev.jamesleach.dynamodb

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class DynamoTestTableOne(
    @get:DynamoDbPartitionKey
    var id: String = "",
    var text: String = ""
)