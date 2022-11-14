package dev.jamesleach.location.googletakeout

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import dev.jamesleach.location.Location
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream

private val log = LoggerFactory.getLogger(TakeoutRecordLocationConsumer::class.java)

@Component
internal class TakeoutRecordLocationConsumer : JsonConsumer {

    private val latitudeLongitudeCorrectionFactor = 10000000.0
    private val accuracyThreshold = 100 // meters

    override fun canConsume(name: String) = name.endsWith("Location History/Records.json")

    override fun consume(inputStream: InputStream) = sequence {
        log.info("Reading record locations")

        val parser = JsonFactory().createParser(inputStream)

        // TODO fewer assumptions

        parser.nextToken()
        parser.nextToken()

        if (parser.currentName() != "locations") {
            throw IllegalStateException("Expected locations but got ${parser.currentName}")
        }
        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw IllegalStateException("Expected START_ARRAY but got ${parser.currentToken}")
        }

        var arrayDepth = 1
        var currentLatitude: Long? = null
        var currentLongitude: Long? = null
        var currentAccuracy: Int? = null

        while (parser.nextToken() != JsonToken.NOT_AVAILABLE && arrayDepth > 0) {
            if (parser.currentToken == JsonToken.START_ARRAY) {
                arrayDepth++
                continue
            }

            if (parser.currentToken == JsonToken.END_ARRAY) {
                arrayDepth--
                continue
            }

            if (parser.currentName == "latitudeE7") {
                currentLatitude = parser.valueAsLong
                continue
            }

            if (parser.currentName == "longitudeE7") {
                currentLongitude = parser.valueAsLong
                continue
            }

            if (parser.currentName == "accuracy") {
                currentAccuracy = parser.valueAsInt
                continue
            }

            if (parser.currentToken == JsonToken.END_OBJECT
                && currentLatitude != null
                && currentLongitude != null
                && currentAccuracy != null
            ) {
                if (currentAccuracy < accuracyThreshold) {
                    yield(
                        toLocation(currentLatitude, currentLongitude)
                    )
                }
                currentLatitude = null
                currentLongitude = null
                currentAccuracy = null
            }
        }
    }

    private fun toLocation(latitude: Long, longitude: Long) = Location(
        latitude / latitudeLongitudeCorrectionFactor,
        longitude / latitudeLongitudeCorrectionFactor
    )
}