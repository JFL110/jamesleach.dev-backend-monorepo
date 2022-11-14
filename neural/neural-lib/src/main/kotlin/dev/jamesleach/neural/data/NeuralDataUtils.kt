package dev.jamesleach.neural.data

import com.google.common.collect.Iterators
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.streams.toList

/**
 * Static collection of utils for data transformation.
 */
object NeuralDataUtils {
    /**
     * index -> array of probabilities with one at the selected index and zeros at the others.
     */
    fun toLabelProbabilityArray(index: Int, numLabels: Int): DoubleArray {
        val out = DoubleArray(numLabels)
        for (i in 0 until numLabels) {
            out[i] = (if (i == index) 1 else 0).toDouble()
        }
        return out
    }

    /**
     * array of probabilities -> index of highest value in the array.
     */
    fun highestProbabilityLabelIndex(labelProbabilities: DoubleArray): Int {
        var maxValue = Double.MIN_VALUE
        var index = -1
        for (i in labelProbabilities.indices) {
            if (labelProbabilities[i] > maxValue) {
                maxValue = labelProbabilities[i]
                index = i
            }
        }
        return index
    }

    /**
     * double array -> reshaped INDArray with one input row.
     */
    fun toSingleInputArray(dataPoint: DataPoint): INDArray {
        val dataShape = dataPoint.dataShape
        val output: INDArray = Nd4j.zeros(1, dataShape.depth, dataShape.height, dataShape.length)
        output.putRow(
            0,
            inInputArrayRow(
                dataShape,
                dataPoint::getInputData3d
            )
        )
        return output
    }

    /**
     * double array -> reshaped INDArray.
     */
    fun inInputArrayRow(dataShape: DataShape, data: (Int, Int, Int) -> Double): INDArray {
        val output: INDArray = Nd4j.zeros(dataShape.depth, dataShape.height, dataShape.length)
        // Input
        // [ [ [a, b], [c, d], [e, f] ]
        //   [ [h, i], [j, k], [l, m] ]
        // to
        // [ [ [ a, c, e ],
        //     [ h, j, l ] ],
        //   [ [ b, d, f ],
        //     [ i, k, m ] ]]
        for (d in 0 until dataShape.depth) {
            for (l in 0 until dataShape.length) {
                for (h in 0 until dataShape.height) {
                    output.putScalar(d.toLong(), h.toLong(), l.toLong(), data(h, l, d))
                }
            }
        }
        return output
    }

    /**
     * TODO
     *
     * @param data
     * @param chunkSize
     * @return
     */
    fun chunkData(data: Stream<LabeledDataPoint>, chunkSize: Int): LabeledDataSetCollection =
        LabeledDataSetCollection(
            StreamSupport.stream<List<LabeledDataPoint>>(
                Spliterators.spliteratorUnknownSize(
                    Iterators.partition(data.iterator(), chunkSize), Spliterator.NONNULL
                ), false
            ) // Batch -> DataSet
                .map { LabeledDataSet(it) }
                .toList()
        )
}

