package dev.jamesleach.neural.net

import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration
import org.deeplearning4j.earlystopping.listener.EarlyStoppingListener
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingGraphTrainer
import org.deeplearning4j.nn.graph.ComputationGraph

/**
 * EarlyStoppingGraphTrainer + NetworkTrainerSpecification
 */
class MetaEquipEarlyStoppingGraphTrainer (
    esConfig: EarlyStoppingConfiguration<ComputationGraph>,
    net: ComputationGraph,
    specification: NetworkTrainerSpecification,
    listener: EarlyStoppingListener<ComputationGraph>?
) : EarlyStoppingGraphTrainer(esConfig, net, specification.trainingData, listener)