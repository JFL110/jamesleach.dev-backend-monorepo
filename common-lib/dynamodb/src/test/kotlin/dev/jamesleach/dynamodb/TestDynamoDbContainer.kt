package dev.jamesleach.dynamodb

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import software.amazon.awssdk.enhanced.dynamodb.Key

/**
 * Test Local DynamoDB setup.
 */
class TestDynamoDbContainer {

    companion object {
        @RegisterExtension
        @JvmField
        val dynamoContainer = DynamoDbContainerExtension(DynamoTestTableOne::class)
    }

    @Test
    fun `Basic CRUD`() {
        val table = dynamoContainer.tableFor(DynamoTestTableOne::class)
        assertEquals("DynamoTestTableOne", table.tableName())

        // Create
        table.putItem(DynamoTestTableOne("abc", "some-text"))

        // Read
        assertEquals(1, table.scan().items().count())
        val readItem = table.getItem(Key.builder().partitionValue("abc").build())
        assertNotNull(readItem)
        assertEquals("abc", readItem.id)
        assertEquals("some-text", readItem.text)

        // Update
        readItem.text = "new-text"
        table.putItem(readItem)
        val readUpdatedItem = table.getItem(Key.builder().partitionValue("abc").build())
        assertEquals("new-text", readUpdatedItem.text)

        // Delete
        table.deleteItem(Key.builder().partitionValue("abc").build())
        assertEquals(0, table.scan().items().count())
        assertNull(table.getItem(Key.builder().partitionValue("abc").build()))
    }
}