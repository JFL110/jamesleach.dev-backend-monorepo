package dev.jamesleach.neural.mnist

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.jamesleach.neural.data.ClassificationOutput
import dev.jamesleach.neural.net.NetworkLoader
import dev.jamesleach.web.JsonErrorResponse
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit

private const val TEST_NETWORK_ID = "test-network-id"

/**
 * End-to-end test of the /mnist project.
 * Train a network on a tiny selection of the MNIST dataset.
 * Input one of the trained values to the endpoint and expect to get the correct digit back.
 */
@TestPropertySource(properties = ["network-id=test-network-id"])
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["network-id=test-network-id"])
@ActiveProfiles("test")
class DigitClassificationIntegrationTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val mNistNetworkFactory: MNistNetworkFactory,
    private val networkLoader: NetworkLoader,
    private val webTestClient: WebTestClient
) {

    @Test
    fun `Correct classifications`() {
        // Create network
        mNistNetworkFactory.createNetwork(
            TEST_NETWORK_ID,
            Duration.of(2, ChronoUnit.SECONDS),
            localResourcesPath("./mnist-ten-rows-train.csv"),
            localResourcesPath("./mnist-ten-rows-test.csv")
        )

        // Verify created network
        val savedNetwork = networkLoader.load(TEST_NETWORK_ID)!!
        assertNotNull(savedNetwork)
        assertNotNull(savedNetwork.graph)
        assertEquals(TEST_NETWORK_ID, savedNetwork.id)
        assertEquals(1, savedNetwork.dataShape.depth)
        assertEquals(28, savedNetwork.dataShape.length)
        assertEquals(28, savedNetwork.dataShape.height)
        assertEquals(3, savedNetwork.dataShape.numDimensions)
        assertEquals(10, savedNetwork.dataShape.numLabels)
        val inputFive = objectMapper.readValue<DigitClassificationInput>(
            localResourcesPath("test-input-five.json").toFile()
        )
        var response = webTestClient
            .post()
            .uri("/classify-digit")
            .bodyValue(inputFive)
            .exchange()
            .expectStatus().isOk
            .expectBody(ClassificationOutput::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(5, response.labelIndex)
        assertTrue(response.labelProbabilities[5] > 0.5)

        val inputOne = objectMapper.readValue<DigitClassificationInput>(
            localResourcesPath("test-input-one.json").toFile()
        )
        response = webTestClient
            .post()
            .uri("/classify-digit")
            .bodyValue(inputOne)
            .exchange()
            .expectStatus().isOk
            .expectBody(ClassificationOutput::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(1, response.labelIndex)
        assertTrue(response.labelProbabilities[1] > 0.5)
    }

    @Test
    fun `Invalid inputs`() {
        // Null input
        webTestClient
            .post()
            .uri("/classify-digit")
            .exchange()
            .expectStatus().is4xxClientError
            .expectBody(JsonErrorResponse::class.java)
            .consumeWith { r ->
                assertNotNull(r.responseBody)
                MatcherAssert.assertThat(
                    r.responseBody?.message,
                    CoreMatchers.containsString("400 Null input")
                )
            }

        // Null array input
        webTestClient
            .post()
            .uri("/classify-digit")
            .bodyValue(DigitClassificationInput(null))
            .exchange()
            .expectStatus().is4xxClientError
            .expectBody(JsonErrorResponse::class.java)
            .isEqualTo(JsonErrorResponse("400 Null input"))

        // Incorrect width input
        webTestClient
            .post()
            .uri("/classify-digit")
            .bodyValue(DigitClassificationInput(Array(28) { DoubleArray(27) }))
            .exchange()
            .expectStatus().is4xxClientError
            .expectBody(JsonErrorResponse::class.java)
            .isEqualTo(JsonErrorResponse("400 Invalid length at row 0 should be 28 but got 27"))

        // Incorrect height input
        webTestClient
            .post()
            .uri("/classify-digit")
            .bodyValue(DigitClassificationInput(Array(27) { DoubleArray(28) }))
            .exchange()
            .expectStatus().is4xxClientError
            .expectBody(JsonErrorResponse::class.java)
            .isEqualTo(JsonErrorResponse("400 Height must be 28 but got 27"))
    }

    @Test
    fun `Ping endpoint returns pong`() {
        webTestClient
            .get()
            .uri("/ping")
            .header("Origin", "http://www.jamesleach.dev")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "**")
            .expectBody(String::class.java)
            .isEqualTo("pong")
    }

    private fun localResourcesPath(fileName: String) = Paths.get(javaClass.classLoader.getResource(fileName)!!.toURI())
}