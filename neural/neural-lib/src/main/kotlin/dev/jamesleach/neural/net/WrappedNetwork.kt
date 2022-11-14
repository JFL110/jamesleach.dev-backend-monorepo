package dev.jamesleach.neural.net

import dev.jamesleach.neural.data.DataShape
import org.deeplearning4j.nn.graph.ComputationGraph

/**
 * Version used to prevent parsing of old saved JSON.
 */
const val JSON_VERSION = 3

/**
 * A [ComputationGraph] + meta data.
 */
data class WrappedNetwork(
    val id: String,
    val dataShape: DataShape,
    val graph: ComputationGraph,
)