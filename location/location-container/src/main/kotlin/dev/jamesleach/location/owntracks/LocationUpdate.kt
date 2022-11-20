package dev.jamesleach.location.owntracks

import com.fasterxml.jackson.annotation.JsonProperty

data class LocationUpdate(
    @JsonProperty("la")
    val latitude: Double,
    @JsonProperty("lo")
    val longitude: Double,
    @JsonProperty("ac")
    val accuracy: Double,
    @JsonProperty("al")
    val altitude: Double,
    @JsonProperty("t")
    val time: Long,
)