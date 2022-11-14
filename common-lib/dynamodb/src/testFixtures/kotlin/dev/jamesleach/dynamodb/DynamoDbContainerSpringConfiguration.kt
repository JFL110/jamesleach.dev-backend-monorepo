package dev.jamesleach.dynamodb

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient


@Component
class DynamoDbContainerSpringConfiguration : TestExecutionListener {

    companion object {
        val containers = ThreadLocal<DynamoDbContainerExtension>()
    }

    @Bean
    fun dynamoEnhancedClient(
        tableSuppliers: Set<DynamoTableClassSupplier>,
        dynamoTableNameResolver: DynamoTableNameResolver
    ): DynamoDbEnhancedClient {
        containers.get().createTables(dynamoTableNameResolver, *tableSuppliers.map { it.getTable() }.toTypedArray())
        return containers.get().enhancedClient
    }

    @Bean
    fun dynamoClient() = containers.get().client

    override fun beforeTestClass(testContext: TestContext) {
        val container = DynamoDbContainerExtension()
        containers.set(container)
        container.start()
    }

    /**
     * Empty tables before each test method.
     */
    override fun beforeTestMethod(testContext: TestContext) {
        containers.get().tables().forEach { table ->
            table.scan().items().forEach { item ->
                table.deleteItem(item)
            }
        }
    }

    override fun afterTestClass(testContext: TestContext) {
        containers.get().close()
    }
}