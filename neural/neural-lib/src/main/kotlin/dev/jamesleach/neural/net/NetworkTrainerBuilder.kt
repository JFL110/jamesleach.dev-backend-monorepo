package dev.jamesleach.neural.net

import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration
import org.deeplearning4j.earlystopping.listener.EarlyStoppingListener
import org.deeplearning4j.earlystopping.saver.InMemoryModelSaver
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator
import org.deeplearning4j.earlystopping.scorecalc.ScoreCalculator
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingGraphTrainer
import org.deeplearning4j.nn.api.Model
import org.deeplearning4j.nn.graph.ComputationGraph
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

private val log = LoggerFactory.getLogger(NetworkTrainerBuilder::class.java)

/**
 * Build an [EarlyStoppingGraphTrainer] to train a [ComputationGraph]
 */
@Component
class NetworkTrainerBuilder {

    /**
     * @param spec the configuration to use to build an [EarlyStoppingGraphTrainer]
     * @return an [EarlyStoppingGraphTrainer] to train a network
     */
    fun trainer(spec: NetworkTrainerSpecification): MetaEquipEarlyStoppingGraphTrainer {
        // Validate
        require(!(spec.compNetworkConfiguration == null && spec.initialModel == null)) { "Must specify one of 'compNetworkConfiguration' or 'initialModel'" }
        require(!(spec.minimumSaveInterval != null && spec.saveModelAsync)) { "Cannot use both minimumSaveInterval and saveModelAsync" }

        // Trainer parts
        val saver: InMemoryModelSaver<ComputationGraph>
        var listener: EarlyStoppingListener<ComputationGraph>? = null
        if (spec.bestModelSaver == null) {
            saver = InMemoryModelSaver<ComputationGraph>()
        } else if (spec.saveModelAsync) {
            val asyncWrapper = AsyncNetworkSaverWrapper(spec.bestModelSaver)
            saver = DelegatingModelSaver(asyncWrapper)
            listener = asyncWrapper
        } else if (spec.minimumSaveInterval != null) {
            val delayWrapper = MinimumIntervalNetworkSaverWrapper(spec.bestModelSaver, spec.minimumSaveInterval)
            saver = DelegatingModelSaver(delayWrapper)
            listener = delayWrapper
        } else {
            saver = DelegatingModelSaver(spec.bestModelSaver)
        }

        val scoreCalculator = spec.evaluationData?.let { DataSetLossCalculator(it, true) } ?: CurrentScore(true)

        // Network parts
        val net: ComputationGraph = spec.initialModel ?: ComputationGraph(spec.compNetworkConfiguration)

        net.init()
        log.info("Network has {} parameters", net.numParams())
        log.info(net.summary())

        return MetaEquipEarlyStoppingGraphTrainer(
            EarlyStoppingConfiguration.Builder<ComputationGraph>()
                .iterationTerminationConditions(
                    MaxTimeIterationTerminationCondition(spec.maxTime.seconds, TimeUnit.SECONDS)
                )
                .scoreCalculator(scoreCalculator)
                .evaluateEveryNEpochs(1)
                .modelSaver(saver)
                .build(),
            net,
            spec,
            listener
        )
    }

    /**
     * ScoreCalculator that uses the last score on the network
     */
    private class CurrentScore<T : Model?>(
        private val minimizeScore: Boolean
    ) : ScoreCalculator<T> {

        override fun calculateScore(network: T): Double {
            return network!!.score()
        }

        override fun minimizeScore() = minimizeScore
    }

    private class DelegatingModelSaver(
        private val delegateBestModelSaver: Consumer<ComputationGraph>
    ) : InMemoryModelSaver<ComputationGraph>() {

        override fun saveBestModel(net: ComputationGraph, score: Double) {
            super.saveBestModel(net, score)
            delegateBestModelSaver.accept(net)
        }

    }
}