package dev.jamesleach.location.googletakeout

import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.location.Location
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
internal class TakeoutPhotoLocationConsumer(
    private val objectMapper: ObjectMapper
) : JsonConsumer {

    override fun canConsume(name: String) = name.contains("Takeout/Google Photos/Photos from ")

    override fun consume(inputStream: InputStream) = sequence {
        val json = objectMapper.readTree(inputStream).get("geoData")
        if (json != null) {
            val latitude = json["latitude"].asDouble()
            val longitude = json["longitude"].asDouble()
            if (latitude != 0.0 && longitude != 0.0) {
                yield(Location(latitude, longitude))
            }
        }
    }
}