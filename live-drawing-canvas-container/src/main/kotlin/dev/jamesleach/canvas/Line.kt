package dev.jamesleach.canvas

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A line on the canvas, possibly being drawn.
 */
data class Line(
    @JsonProperty("d")
    val userId: String,

    @JsonProperty("n")
    val ownerLineNumber: Int,

    @JsonProperty("zindex")
    val zIndex: Int,

    @JsonProperty("p")
    val points: MutableList<Point>,

    @JsonProperty("r")
    val brushRadius: Int?,

    @JsonProperty("c")
    val brushColor: String?,

    @JsonProperty("f")
    val finished: Boolean?
)