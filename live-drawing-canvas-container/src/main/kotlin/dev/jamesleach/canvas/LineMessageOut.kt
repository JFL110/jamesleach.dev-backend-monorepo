package dev.jamesleach.canvas

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Message returned to all subscribers when a line is modified.
 *
 * @author jim
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LineMessageOut(
    @JsonProperty("d")
    val clientId: String,

    @JsonProperty("t")
    val type: String,

    @JsonProperty("n")
    val clientLineNumber: Int,

    @JsonProperty("p")
    val points: List<Point>?,

    @JsonProperty("c")
    val brushColor: String?,

    @JsonProperty("r")
    val brushRadius: Int?,

    @JsonProperty("i")
    val pointsIndexStart: Int?,

    @JsonProperty("f")
    val isFinished: Boolean?,
){
    fun pointsNonNull() = points ?: listOf()
}