package dev.jamesleach.neural.net

/**
 * Default learning rate
 */
const val DEFAULT_LEARNING_RATE = 0.005

/**
 * Hyper-parameters and configuration found on all networks.
 */
data class CommonNetSpecification(
    val seed: Long = 1234,
    val dropout: Double = 0.2,
    val learningRate: Double = DEFAULT_LEARNING_RATE,
    val sizeMultiplier: Int = 1,
)