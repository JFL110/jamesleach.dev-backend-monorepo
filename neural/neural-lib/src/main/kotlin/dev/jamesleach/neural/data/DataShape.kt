package dev.jamesleach.neural.data

/**
 * Description of the shape of input data to a neural network.
 */
data class DataShape(
    val numDimensions: Int,
    val numLabels: Int,
    val length: Int,
    val height: Int,
    val depth: Int,
)