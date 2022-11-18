package dev.jamesleach.neural.mnist

import dev.jamesleach.neural.data.LabeledDataPoint
import dev.jamesleach.neural.data.LabeledDataSetCollection
import dev.jamesleach.neural.data.NeuralDataUtils
import dev.jamesleach.neural.net.*
import org.deeplearning4j.nn.graph.ComputationGraph
import org.nd4j.evaluation.classification.Evaluation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

private const val HEIGHT = 28
private const val WIDTH = 28
private const val MAX_VALUE = 255
private const val NUM_DIGITS = 10
private val log = LoggerFactory.getLogger(MNistNetworkFactory::class.java)

/**
 * Train and save a network using the MNIST dataset.
 */
@Service
class MNistNetworkFactory(
    val saver: NetworkSaver,
    val loader: NetworkLoader,
    val trainerBuilder: NetworkTrainerBuilder,
) {

    fun createNetwork(networkId: String, timeToSpend: Duration, trainingDataCsv: Path, testingDataCsv: Path) {
        // Parse training data
        val trainingData = readDataFromCsv(trainingDataCsv)

        // Define network & training regime
        val trainerSpec = NetworkTrainerSpecification(
            initialModel = loader.load(networkId)?.let(WrappedNetwork::graph),
            compNetworkConfiguration = MNistFeedForward().build(
                // Use default learning rate and dropout
                CommonNetSpecification(),
                trainingData.dataShape!!
            ),
            trainingData = trainingData.toDataSetIterator(),
            maxTime = timeToSpend,
            bestModelSaver = { graph ->
                saver.save(
                    WrappedNetwork(
                        networkId,
                        trainingData.dataShape!!,
                        graph
                    )
                )
            }
        )
        val trainer = trainerBuilder.trainer(trainerSpec)

        // Train
        log.info("Spending {} seconds on training", timeToSpend.seconds)
        val net: ComputationGraph = trainer.fit().bestModel

        // Evaluate
        log.info(net.evaluate<Evaluation>(trainingData.toDataSetIterator()).toString())
        log.info(net.evaluate<Evaluation>(readDataFromCsv(testingDataCsv).toDataSetIterator()).toString())
        trainerSpec.bestModelSaver!!.accept(net)
    }

    /**
     * Read, parse and batch all CSV lines.
     */
    private fun readDataFromCsv(csvPath: Path): LabeledDataSetCollection {
        log.info("Loading csv data...")
        return NeuralDataUtils.chunkData(
            Files.readAllLines(csvPath)
                .stream() // Skip the header row
                .skip(1)
                .parallel() // Parse
                .map { l: String -> csvRowToDataPoint(l) }, 1000
        )
    }

    /**
     * Single CSV row to a data point with label.
     */
    private fun csvRowToDataPoint(l: String): LabeledDataPoint {
        val parts = l.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val labelValue = parts[0].toInt()
        val input = Array(HEIGHT) { Array(WIDTH) { DoubleArray(1) } }
        for (h in 0 until HEIGHT) {
            for (w in 0 until WIDTH) {
                input[h][w][0] = parts[1 + (w * WIDTH + h)].toInt() / MAX_VALUE.toDouble()
            }
        }
        return LabeledDataPoint(input, NeuralDataUtils.toLabelProbabilityArray(labelValue, NUM_DIGITS))
    }
}