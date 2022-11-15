package dev.jamesleach

import dev.jamesleach.dynamodb.DynamoDbContainerSpringConfiguration
import dev.jamesleach.web.StatusDto
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.web.reactive.server.WebTestClient


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["version-file=src/test/resources/test-version-information.txt"]
)
@TestExecutionListeners(DynamoDbContainerSpringConfiguration::class)
@ActiveProfiles("test")
class TestStatusEndpoint @Autowired constructor(
    val webTestClient: WebTestClient
) {
    @Test
    fun `status response contains expected values`() {
        webTestClient.get().uri("/status").exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(StatusDto::class.java).consumeWith {
                assertThat(it.responseBody?.uptime).matches("00:00:0.*")
                assertEquals("version-information", it.responseBody?.versionInformation)
            }
    }
}