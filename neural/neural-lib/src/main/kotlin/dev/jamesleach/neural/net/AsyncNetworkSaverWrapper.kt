package dev.jamesleach.neural.net

import com.google.common.base.Stopwatch
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration
import org.deeplearning4j.earlystopping.EarlyStoppingResult
import org.deeplearning4j.earlystopping.listener.EarlyStoppingListener
import org.deeplearning4j.nn.graph.ComputationGraph
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

private val log = LoggerFactory.getLogger(AsyncNetworkSaverWrapper::class.java)
private val MAX_SYNC_WAIT = Duration.ofMinutes(5)

internal class AsyncNetworkSaverWrapper(private val delegate: Consumer<ComputationGraph>) :
    Consumer<ComputationGraph>,
    EarlyStoppingListener<ComputationGraph> {

    private val nextModelToSave = AtomicReference<ComputationGraph>()
    private val saveLock: Lock = ReentrantLock()
    private val saving = AtomicBoolean()

    override fun accept(graph: ComputationGraph) {
        nextModelToSave.set(graph)
        asyncSaveIfNotBusy(false)
    }

    override fun onStart(esConfig: EarlyStoppingConfiguration<ComputationGraph>, net: ComputationGraph?) {}

    override fun onEpoch(
        epochNum: Int,
        score: Double,
        esConfig: EarlyStoppingConfiguration<ComputationGraph?>,
        net: ComputationGraph?
    ) = asyncSaveIfNotBusy(false)

    override fun onCompletion(esResult: EarlyStoppingResult<ComputationGraph>) = asyncSaveIfNotBusy(true)

    private fun doSave(graph: ComputationGraph) {
        try {
            delegate.accept(graph)
        } finally {
            saving.set(false)
        }
    }

    private fun asyncSaveIfNotBusy(forceWait: Boolean) {
        saveLock.lock()
        try {
            if (saving.get()) {
                if (!forceWait) {
                    return
                }
                val stopwatch = Stopwatch.createStarted()
                while (saving.get()) {
                    Thread.onSpinWait()
                    if (stopwatch.elapsed().seconds > MAX_SYNC_WAIT.seconds) {
                        log.error(
                            "Gave up waiting to save network - waited for {} seconds",
                            stopwatch.elapsed(TimeUnit.SECONDS)
                        )
                    }
                }
            }
            saving.set(true)
            val graph = nextModelToSave.getAndSet(null) ?: return
            Thread { doSave(graph) }.start()
        } finally {
            saveLock.unlock()
        }
    }
}