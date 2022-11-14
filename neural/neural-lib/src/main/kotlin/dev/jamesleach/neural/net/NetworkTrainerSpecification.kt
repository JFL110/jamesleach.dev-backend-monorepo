package dev.jamesleach.neural.net

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration
import org.deeplearning4j.nn.graph.ComputationGraph
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import java.time.Duration
import java.util.function.Consumer

/**
 * Configuration for [NetworkTrainerBuilder]
 */
data class NetworkTrainerSpecification(
    val initialModel: ComputationGraph? = null,
    val compNetworkConfiguration: ComputationGraphConfiguration? = null,
    val maxTime: Duration,
    val trainingData: DataSetIterator? = null,
    val evaluationData: DataSetIterator? = null,
    val bestModelSaver: Consumer<ComputationGraph>? = null,
    val saveModelAsync: Boolean = false,
    val minimumSaveInterval: Duration? = null
)