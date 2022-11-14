package dev.jamesleach.neural.data

/**
 * Network output to a classification problem.
 */
data class ClassificationOutput(
    val labelProbabilities: DoubleArray,
    val labelIndex: Int
)