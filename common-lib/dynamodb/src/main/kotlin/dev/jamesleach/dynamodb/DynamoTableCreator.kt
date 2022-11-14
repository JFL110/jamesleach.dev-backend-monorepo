package dev.jamesleach.dynamodb

import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import kotlin.reflect.KClass

@Component
class DynamoTableCreator(
    private val dynamoTableNameResolver: DynamoTableNameResolver,
    private val enhancedClient: DynamoDbEnhancedClient,
    private val dynamoDbClient: DynamoDbClient
) {
    fun createTables(vararg tableClasses: KClass<*>) =
        tableClasses.map(this::createTable).toList()

    fun createTable(tableClass: KClass<*>): DynamoDbTable<out Any> {
        val table =
            enhancedClient.table(dynamoTableNameResolver.getName(tableClass), TableSchema.fromClass(tableClass.java))
        table.createTable(
            CreateTableEnhancedRequest.builder()
                .provisionedThroughput(
                    ProvisionedThroughput.builder()
                        .readCapacityUnits(1).writeCapacityUnits(1).build()
                ).build()
        )
        dynamoDbClient.waiter().waitUntilTableExists { t -> t.tableName(dynamoTableNameResolver.getName(tableClass)) }
        return table
    }
}