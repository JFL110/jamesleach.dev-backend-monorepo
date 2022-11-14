package dev.jamesleach.neural.net

import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration
import org.deeplearning4j.earlystopping.EarlyStoppingResult
import org.deeplearning4j.earlystopping.listener.EarlyStoppingListener
import org.deeplearning4j.nn.graph.ComputationGraph
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * Training listener that saves the network every x durations.
 */
internal class MinimumIntervalNetworkSaverWrapper(
    private val delegate: Consumer<ComputationGraph>,
    private val saveEvery: Duration
) : Consumer<ComputationGraph>,
    EarlyStoppingListener<ComputationGraph> {

    private val lastNetworkToSave: AtomicReference<ComputationGraph> = AtomicReference<ComputationGraph>()
    private val lastSaveTime: AtomicReference<ZonedDateTime> = AtomicReference<ZonedDateTime>()

    override fun accept(graph: ComputationGraph) {
        val now = ZonedDateTime.now()
        lastSaveTime.getAndUpdate { previous ->
            // Ignore check
            if (previous != null && Duration.between(previous, now).seconds < saveEvery.seconds) {
                lastNetworkToSave.set(graph)
                return@getAndUpdate previous
            }
            lastNetworkToSave.set(null)
            delegate.accept(graph)
            now
        }
    }

    override fun onStart(esConfig: EarlyStoppingConfiguration<ComputationGraph>, net: ComputationGraph) {
        lastSaveTime.getAndUpdate { previous: ZonedDateTime? -> previous ?: ZonedDateTime.now() }
    }

    override fun onEpoch(
        epochNum: Int,
        score: Double,
        esConfig: EarlyStoppingConfiguration<ComputationGraph>,
        net: ComputationGraph
    ) = delayedCheck(false)

    override fun onCompletion(esResult: EarlyStoppingResult<ComputationGraph>) = delayedCheck(true)

    private fun delayedCheck(force: Boolean) {
        val now = ZonedDateTime.now()
        lastSaveTime.getAndUpdate { previous ->
            // Ignore check
            if (!force && previous != null && Duration.between(previous, now).seconds < saveEvery.seconds) {
                return@getAndUpdate previous
            }
            lastNetworkToSave.getAndUpdate { graph: ComputationGraph? ->
                graph?.let(delegate::accept)
                null
            }
            now
        }
    }
}