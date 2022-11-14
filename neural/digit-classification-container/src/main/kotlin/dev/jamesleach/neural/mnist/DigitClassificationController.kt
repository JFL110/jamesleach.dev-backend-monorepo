package dev.jamesleach.neural.mnist

import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.neural.data.ClassificationOutput
import dev.jamesleach.neural.data.DataShape
import dev.jamesleach.neural.data.UnlabeledDataPoint
import dev.jamesleach.neural.net.SavedNetworkRunner
import dev.jamesleach.web.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.ContextStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val log = LoggerFactory.getLogger(DigitClassificationController::class.java)
private const val MAX_PIXEL_VALUE = 255
private val MNIST_DATA_SHAPE: DataShape = DataShape(3, 9, 28, 28, 1)

/**
 * /classify-digit endpoint
 * Accept a pixel map, pass through a network and return the network prediction.
 */
@RestController
class DigitClassificationController(
    @Value("\${network-id}")
    val networkId: String,
    val objectMapper: ObjectMapper,
    val savedNetworkRunner: SavedNetworkRunner,
) {

    @PostMapping("/classify-digit")
    fun classifyDigit(@RequestBody input: DigitClassificationInput?): ClassificationOutput {
        // Validate
        if (input?.pixels == null) {
            throw BadRequestException("Null input")
        }
        if (input.pixels.size != MNIST_DATA_SHAPE.height) {
            throw BadRequestException("Height must be ${MNIST_DATA_SHAPE.height} but got ${input.pixels.size}")
        }

        // Add the 3rd dimension
        val as3D =
            Array(MNIST_DATA_SHAPE.height) { Array(MNIST_DATA_SHAPE.length) { DoubleArray(MNIST_DATA_SHAPE.depth) } }
        for (h in 0 until MNIST_DATA_SHAPE.height) {
            if (input.pixels[h].size != MNIST_DATA_SHAPE.length) {
                throw BadRequestException("Invalid length at row $h should be ${MNIST_DATA_SHAPE.length} but got ${input.pixels[h].size}")
            }
            for (l in 0 until MNIST_DATA_SHAPE.length) {
                as3D[h][l] = doubleArrayOf(input.pixels[h][l] / MAX_PIXEL_VALUE.toDouble())
            }
        }

        // Classify
        val point = UnlabeledDataPoint(as3D, MNIST_DATA_SHAPE)
        return savedNetworkRunner.runClassification(networkId, point)
    }

    @Async
    @EventListener
    fun handleContextStart(cse: ContextStartedEvent?) {
        log.info("Running warmup point through network")
        val warmupPoint = objectMapper.readValue(
            ClassPathResource("warmup-point.json").inputStream,
            DigitClassificationInput::class.java
        )
        classifyDigit(warmupPoint)
    }
}