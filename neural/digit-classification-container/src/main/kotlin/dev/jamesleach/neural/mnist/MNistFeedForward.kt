package dev.jamesleach.neural.mnist

import dev.jamesleach.neural.data.DataShape
import dev.jamesleach.neural.net.CommonNetSpecification
import dev.jamesleach.neural.net.NetworkConfigurationBuilder
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.dropout.GaussianDropout
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions

/**
 * Very simple feed-forward network accepting a convolution-style input.
 */
internal class MNistFeedForward : NetworkConfigurationBuilder {
    override fun build(commonNetConfig: CommonNetSpecification, dataShape: DataShape): ComputationGraphConfiguration =
        NeuralNetConfiguration.Builder()
            .seed(commonNetConfig.seed)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(Adam(commonNetConfig.learningRate))
            .graphBuilder()
            .addInputs("img1")
            .setInputTypes(
                InputType.convolutional(
                    dataShape.height.toLong(),
                    dataShape.length.toLong(),
                    dataShape.depth.toLong()
                )
            )
            .addLayer(
                "d1",
                DenseLayer.Builder()
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .dropOut(GaussianDropout(commonNetConfig.dropout))
                    .nOut(200)
                    .build(),
                "img1"
            )
            .addLayer(
                "output",
                OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                    .activation(Activation.SOFTMAX)
                    .weightInit(WeightInit.XAVIER)
                    .nOut(dataShape.numLabels)
                    .dropOut(GaussianDropout(commonNetConfig.dropout))
                    .build(),
                "d1"
            )
            .setOutputs("output")
            .build()
}