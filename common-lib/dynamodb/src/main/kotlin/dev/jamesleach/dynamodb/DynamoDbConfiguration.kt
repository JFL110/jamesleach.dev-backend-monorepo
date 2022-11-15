package dev.jamesleach.dynamodb

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

@Configuration
@ConfigurationProperties
@Profile("!test")
class DynamoDbConfiguration(
    @Value("\${dynamodb.endpoint:}") val endpoint: String?,
    @Value("\${dynamodb.accessKey:}") val accessKey: String?,
    @Value("\${dynamodb.secretKey:}") val secretKey: String?,
    @Value("\${dynamodb.region}") val region: String
) {
    @Bean
    fun amazonDynamoDB(): DynamoDbClient {

        val client = DynamoDbClient.builder()
            .credentialsProvider(
                if (accessKey.isNullOrBlank() && secretKey.isNullOrBlank())
                    DefaultCredentialsProvider.builder().build()
                else
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                    )
            )

        if (region.isNotEmpty()) {
            client.region(Region.of(region))
        }
        if (endpoint != null && endpoint!!.isNotEmpty()) {
            client.endpointOverride(URI.create(endpoint!!))
        }

        return client.build()
    }

    @Bean
    fun enhancedClient(dynamoDbClient: DynamoDbClient): DynamoDbEnhancedClient =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build()

}