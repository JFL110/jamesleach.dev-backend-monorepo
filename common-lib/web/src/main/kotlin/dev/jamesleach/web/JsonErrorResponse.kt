package dev.jamesleach.web

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * JSON format for error responses.
 */
data class JsonErrorResponse @JsonCreator constructor(
    @JsonProperty("message") val message: String?
)