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
class TestMessageEndpoint @Autowired constructor(
    val webTestClient: WebTestClient
) {
    @Test
    fun `Create and retrieve message`() {
        webTestClient.post().uri("/message")
            .bodyValue(MessageDto("msg-1", "hello"))
            .exchange()
            .expectStatus().isEqualTo(200)

        webTestClient.get().uri("/message/msg-1")
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody().json("{\"id\":\"msg-1\",\"text\":\"hello\"}")
    }
}