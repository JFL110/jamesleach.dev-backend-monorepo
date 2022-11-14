package dev.jamesleach.neural.data

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.adapter.SingletonDataSetIterator
import org.nd4j.linalg.factory.Nd4j

/**
 * A collection of labeled data points for training a neural network.
 */
class LabeledDataSet(data: List<LabeledDataPoint>) {
    val dataShape: DataShape
    val dataSet: DataSet

    /**
     * Create a Deeplearning4j Dataset from a list of LabeledDataPoint.
     */
    init {
        check(data.isNotEmpty()) { "Empty data" }
        dataShape = data[0].dataShape
        check(!(dataShape.numDimensions != 3)) { "Only 3d data implemented" }
        val outputNDArray: INDArray = Nd4j.zeros(data.size, dataShape.numLabels)
        val inputNDArray: INDArray =
            Nd4j.zeros(data.size, dataShape.depth, dataShape.height, dataShape.length)
        for (i in data.indices) {
            inputNDArray.putRow(
                i.toLong(),
                NeuralDataUtils.inInputArrayRow(
                    dataShape,
                    data[i]::getInputData3d
                )
            )
            outputNDArray.putRow(i.toLong(), Nd4j.create(data[i].labels, intArrayOf(dataShape.numLabels)))
        }
        dataSet = DataSet(inputNDArray, outputNDArray)
    }

    fun toSingletonIterator() = SingletonDataSetIterator(dataSet)
}