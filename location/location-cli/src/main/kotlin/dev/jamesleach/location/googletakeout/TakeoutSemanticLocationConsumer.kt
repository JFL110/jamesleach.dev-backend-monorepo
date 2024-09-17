package dev.jamesleach.location.googletakeout

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.jamesleach.location.Location
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
internal class TakeoutSemanticLocationConsumer(
    private val objectMapper: ObjectMapper
) : JsonConsumer {

    private val latitudeLongitudeCorrectionFactor = 10000000.0
    private val minAccuracyMeters = 300;

    override fun canConsume(name: String) = name.contains("Takeout/Location History (Timeline)/Semantic Location History")

    override fun consume(inputStream: InputStream) = sequence {
        objectMapper.readTree(inputStream)["timelineObjects"].forEach { timelineObject ->
            timelineObject["activitySegment"]?.let { activitySegment ->
                activitySegment["startLocation"]?.let { start ->
                    toLocation(
                        start["latitudeE7"],
                        start["longitudeE7"]
                    )?.let { yield(it) }
                }
                activitySegment["endLocation"]?.let { end ->
                    toLocation(
                        end["latitudeE7"],
                        end["longitudeE7"]
                    )?.let { yield(it) }
                }

                activitySegment["waypointPath"]?.get("waypoints")?.forEach { waypoint ->
                    toLocation(waypoint["latE7"], waypoint["lngE7"])?.let { yield(it) }
                }

                activitySegment["simplifiedRawPath"]?.get("points")?.filter { point ->
                    val accuracy = point.get("accuracyMeters")?.intValue()
                    accuracy != null && accuracy < minAccuracyMeters
                }?.forEach { point ->
                    toLocation(point["latE7"], point["lngE7"])?.let { yield(it) }
                }
            }

            timelineObject["placeVisit"]?.get("location")?.let { location ->
                toLocation(
                    location["latitudeE7"],
                    location["longitudeE7"]
                )?.let { yield(it) }
            }
        }
    }

    private fun toLocation(latitude: JsonNode?, longitude: JsonNode?) =
        if (latitude == null || longitude == null) null else toLocation(latitude.asLong(), longitude.asLong())

    private fun toLocation(latitude: Long, longitude: Long) = Location(
        latitude / latitudeLongitudeCorrectionFactor,
        longitude / latitudeLongitudeCorrectionFactor
    )
}