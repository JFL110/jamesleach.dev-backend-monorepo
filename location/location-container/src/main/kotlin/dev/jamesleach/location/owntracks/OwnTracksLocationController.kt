package dev.jamesleach.location.owntracks

import dev.jamesleach.web.BadRequestException
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Controller to log location updates from an OwnTracks Android App.
 */
@RestController
class OwnTracksLocationController(
    @Value("\${owntracks.userKey}") val userKey: String,
    private val locationHistoryDao: LocationHistoryDao
) {
    @PostMapping("/locations")
    fun recordLocation(@RequestBody locationUpdate: OwnTracksLocationUpdateDto): RecordLocationResponse {
        if (locationUpdate.topic != "owntracks/user/$userKey") {
            throw BadRequestException("Invalid user key from topic ${locationUpdate.topic}")
        }

        locationHistoryDao.createSinglePending(
            LocationUpdate(
                longitude = locationUpdate.lon,
                latitude = locationUpdate.lat,
                accuracy = locationUpdate.acc,
                time = locationUpdate.tst,
                altitude = locationUpdate.alt
            )
        )

        return RecordLocationResponse(200)
    }


    data class RecordLocationResponse(
        val status: Int
    )
}