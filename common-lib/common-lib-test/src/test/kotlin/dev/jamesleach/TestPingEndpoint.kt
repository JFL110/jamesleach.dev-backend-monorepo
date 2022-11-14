package dev.jamesleach

import dev.jamesleach.dynamodb.DynamoDbContainerSpringConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.web.reactive.server.WebTestClient


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestExecutionListeners(DynamoDbContainerSpringConfiguration::class)
@ActiveProfiles("test")
class TestPingEndpoint @Autowired constructor(
    val webTestClient: WebTestClient
) {
    @Test
    fun `Ping controller returns pong`() {
        webTestClient.get().uri("/ping").exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(String::class.java).isEqualTo("pong")
    }

    @Test
    fun `Unknown endpoint returns 404`() {
        webTestClient.get().uri("/unknown-endpoint").exchange()
            .expectStatus().isEqualTo(404)
    }
}