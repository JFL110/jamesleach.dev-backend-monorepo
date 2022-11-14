package dev.jamesleach.canvas

/**
 * Message to describe complete canvas state to new users.
 */
data class InitialCanvasMessage(
    val userId: String,
    val lines: List<Line>,
)