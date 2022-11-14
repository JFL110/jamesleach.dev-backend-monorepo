package dev.jamesleach.dynamodb

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.dynamodb.DynaliteContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import java.net.URI
import kotlin.reflect.KClass

/**
 * JUnit5 Extension to
 * - Manage a local DynamoDB Docker container (Dynalite)
 * - Create any specified DynamoDB tables
 */
class DynamoDbContainerExtension(
    private vararg val tableClasses: KClass<*>
) : BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private val dynalite =
        DynaliteContainer(DockerImageName.parse("quay.io/testcontainers/dynalite").withTag("v1.2.1-1"))
    private val tables = mutableMapOf<KClass<*>, DynamoDbTable<*>>()

    val client by lazy {
        createClient()
    }
    val enhancedClient: DynamoDbEnhancedClient by lazy {
        createEnhancedClient()
    }

    fun <T : Any> tableFor(clazz: KClass<T>): DynamoDbTable<T> = tables[clazz] as DynamoDbTable<T>

    fun tables() : List<DynamoDbTable<Any>> = tables.values.map { it as DynamoDbTable<Any> }

    override fun beforeAll(context: ExtensionContext?) {
        start()
        tableClasses.forEach {
           val table = DynamoTableCreator(DefaultDynamoTableNameResolver(), enhancedClient, client)
            .createTable(it)
            tables[it] = table
        }
    }

    fun start() {
        dynalite.start()
    }

    override fun close() {
        dynalite.stop()
    }

    fun createTables(dynamoTableNameResolver: DynamoTableNameResolver, vararg tableClasses: KClass<*>) {
         tableClasses.forEach {
            val table = enhancedClient.table(dynamoTableNameResolver.getName(it), TableSchema.fromClass(it.java))
            tables[it] = table
            table.createTable(
                CreateTableEnhancedRequest.builder()
                    .provisionedThroughput(
                        ProvisionedThroughput.builder()
                            .readCapacityUnits(1).writeCapacityUnits(1).build()
                    ).build()
            )
            client.waiter().waitUntilTableExists { t -> t.tableName(dynamoTableNameResolver.getName(it)) }
        }
    }

    private fun createClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .endpointOverride(URI.create(dynalite.endpointConfiguration.serviceEndpoint))
            .region(software.amazon.awssdk.regions.Region.EU_WEST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("dummy-key", "dummy-secret")
                )
            )
            .build()
    }

    private fun createEnhancedClient() = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(client)
        .build()
}