package dev.jamesleach.neural.data

/**
 * Implementation of [DataPoint] with a label value for training.
 */
class LabeledDataPoint(
    /**
     * height-length-depth
     */
    val inputData3d: Array<Array<DoubleArray>>,
    val labels: DoubleArray,
) : DataPoint {

    override val dataShape: DataShape
        get() {
            val height = inputData3d.size
            val length = if (height == 0) 0 else inputData3d[0].size
            val depth = if (length == 0) 0 else inputData3d[0][0].size
            val numLabels = labels.size
            return DataShape(
                3,
                numLabels,
                length,
                height,
                depth
            )
        }

    override fun getInputData3d(height: Int, width: Int, depth: Int): Double {
        return inputData3d[height][width][depth]
    }
}