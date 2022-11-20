package dev.jamesleach.location.owntracks

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Message format from the OwnTracks app.
 *
 *  Example message:
 *  {
 *      "_type": "location",
 *      "acc": 6,
 *      "alt": 92,
 *      "batt": 0,
 *      "created_at": 1668902835,
 *      "lat": 53.4287668,
 *      "lon": -2.2194535,
 *      "t": "u",
 *      "tid": "d",
 *      "topic": "owntracks/user/abc",
 *      "tst": 1668902814,
 *      "vac": 1,
 *      "vel": 0
 *  }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OwnTracksLocationUpdateDto(
    val acc: Double,
    val alt: Double,
    val lat: Double,
    val lon: Double,
    val created_at: Long,
    val _type: String,
    val tst: Long,
    val tid: String,
    val topic: String,
)