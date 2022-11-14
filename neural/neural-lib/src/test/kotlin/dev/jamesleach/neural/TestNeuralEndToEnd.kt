package dev.jamesleach.neural

import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableList
import dev.jamesleach.neural.data.LabeledDataPoint
import dev.jamesleach.neural.data.NeuralDataUtils.highestProbabilityLabelIndex
import dev.jamesleach.neural.data.NeuralDataUtils.toSingleInputArray
import dev.jamesleach.neural.data.UnlabeledDataPoint
import dev.jamesleach.neural.net.*
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.dropout.GaussianDropout
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.graph.ComputationGraph
import org.deeplearning4j.nn.weights.WeightInit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger(TestNeuralEndToEnd::class.java)
private const val NET_ID = "net-id"

class TestNeuralEndToEnd {

    private val networkLoader = mock<NetworkLoader>()
    private val trainerBuilder = NetworkTrainerBuilder()
    private val savedNetworkRunner = SavedNetworkRunner(networkLoader)

    @Test
    fun `Train and evaluate a simple problem`() {
        // Test data
        val pointData1 = arrayOf(
            arrayOf(doubleArrayOf(0.7), doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.5)),
            arrayOf(doubleArrayOf(0.7), doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.5))
        )
        val pointData2 = arrayOf(
            arrayOf(doubleArrayOf(0.5), doubleArrayOf(0.75), doubleArrayOf(0.5), doubleArrayOf(0.5)),
            arrayOf(doubleArrayOf(0.5), doubleArrayOf(0.75), doubleArrayOf(0.5), doubleArrayOf(0.5))
        )
        val pointData3 = arrayOf(
            arrayOf(doubleArrayOf(0.4), doubleArrayOf(0.4), doubleArrayOf(0.65), doubleArrayOf(0.4)),
            arrayOf(doubleArrayOf(0.4), doubleArrayOf(0.4), doubleArrayOf(0.65), doubleArrayOf(0.4))
        )
        val pointData4 = arrayOf(
            arrayOf(doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.8)),
            arrayOf(doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.84))
        )
        val point1 = LabeledDataPoint(pointData1, doubleArrayOf(1.0, 0.0, 0.0, 0.0))
        val point2 = LabeledDataPoint(pointData2, doubleArrayOf(0.0, 1.0, 0.0, 0.0))
        val point3 = LabeledDataPoint(pointData3, doubleArrayOf(0.0, 0.0, 1.0, 0.0))
        val point4 = LabeledDataPoint(pointData4, doubleArrayOf(0.0, 0.0, 0.0, 1.0))
        val dataSet = dev.jamesleach.neural.data.LabeledDataSet(ImmutableList.of(point1, point2, point3, point4))

        // Network
        var savedModel: ComputationGraph? = null
        val timer = Stopwatch.createStarted()

        val trainer = trainerBuilder.trainer(
            NetworkTrainerSpecification(
                compNetworkConfiguration = FeedForwardNetwork().build(
                    CommonNetSpecification(),
                    dataSet.dataShape
                ),
                bestModelSaver = { savedModel = it },
                maxTime = Duration.of(10, ChronoUnit.SECONDS),
                trainingData = dataSet.toSingletonIterator()
            )
        )

        // Train
        val bestModel = trainer.fit().bestModel
        assertNotNull(savedModel)
        assertTrue(timer.elapsed(TimeUnit.SECONDS) < 15)

        // Evaluate
        val evaluation = bestModel.evaluate<Evaluation>(dataSet.toSingletonIterator())
        log.info(evaluation.toString())
        assertTrue(evaluation.accuracy() > 0.5)
        assertTrue(evaluation.precision() > 0.5)
        assertTrue(evaluation.recall() > 0.5)
        assertTrue(evaluation.f1() > 0.5)

        val pointOneOutput = bestModel.output(toSingleInputArray(point1))[0]
        assertArrayEquals(longArrayOf(1, 4), pointOneOutput.shape())
        assertEquals(0, highestProbabilityLabelIndex(pointOneOutput.toDoubleVector()))

        // Runner
        `when`(networkLoader.load(NET_ID)).thenReturn(WrappedNetwork(NET_ID, dataSet.dataShape, bestModel))
        assertEquals(0, savedNetworkRunner.runClassification(NET_ID, point1).labelIndex)
        assertEquals(1, savedNetworkRunner.runClassification(NET_ID, point2).labelIndex)
        assertEquals(2, savedNetworkRunner.runClassification(NET_ID, point3).labelIndex)
        assertEquals(3, savedNetworkRunner.runClassification(NET_ID, point4).labelIndex)
        assertEquals(0, highestProbabilityLabelIndex(savedNetworkRunner.runClassification(NET_ID, point1).labelProbabilities))

        val point5 = UnlabeledDataPoint(
            arrayOf(
                arrayOf(doubleArrayOf(0.65), doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.5)),
                arrayOf(doubleArrayOf(0.76), doubleArrayOf(0.5), doubleArrayOf(0.5), doubleArrayOf(0.5))
            ),
            dataSet.dataShape
        )
        assertEquals(0, savedNetworkRunner.runClassification(NET_ID, point5).labelIndex)
    }

    private class FeedForwardNetwork : NetworkConfigurationBuilder {
        override fun build(
            commonNetConfig: CommonNetSpecification,
            dataShape: dev.jamesleach.neural.data.DataShape
        ): ComputationGraphConfiguration {
            return NeuralNetConfiguration.Builder()
                .seed(commonNetConfig.seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Adam(commonNetConfig.learningRate))
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(
                    InputType.convolutional(
                        dataShape.height.toLong(),
                        dataShape.length.toLong(),
                        dataShape.depth.toLong()
                    )
                )
                .addLayer(
                    "l1",
                    DenseLayer.Builder()
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .dropOut(GaussianDropout(commonNetConfig.dropout))
                        .nOut(10)
                        .build(),
                    "input"
                )
                .addLayer(
                    "output",
                    OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .nOut(dataShape.numLabels)
                        .dropOut(GaussianDropout(commonNetConfig.dropout))
                        .build(),
                    "l1"
                )
                .setOutputs("output")
                .build()
        }
    }
}