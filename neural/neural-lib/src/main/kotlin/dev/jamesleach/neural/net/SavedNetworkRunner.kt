package dev.jamesleach.neural.net

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import dev.jamesleach.neural.data.ClassificationOutput
import dev.jamesleach.neural.data.DataPoint
import dev.jamesleach.neural.data.NeuralDataUtils
import org.springframework.stereotype.Component

private const val MAX_CACHED_NETWORKS: Long = 100

/**
 * Run a single data point through a saved network.
 * Hard-cache the network based on ID.
 */
@Component
class SavedNetworkRunner(
    private val loader: NetworkLoader
) {

    private val networkCache = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHED_NETWORKS)
        .build(CacheLoader.from { id: String -> loadNetwork(id) })

    /**
     * Run a single data point through a saved network.
     */
    fun runClassification(networkId: String, dataPoint: DataPoint): ClassificationOutput {
        val network = networkCache.getUnchecked(networkId)
        val input = NeuralDataUtils.toSingleInputArray(dataPoint)
        val output = network.graph.output(input)[0].toDoubleVector() // Only one output supported
        val predictedIndex = NeuralDataUtils.highestProbabilityLabelIndex(output)
        return ClassificationOutput(
            output,
            predictedIndex
        )
    }

    private fun loadNetwork(id: String) =
        loader.load(id) ?: throw IllegalStateException("No network found for id '$id'")
}