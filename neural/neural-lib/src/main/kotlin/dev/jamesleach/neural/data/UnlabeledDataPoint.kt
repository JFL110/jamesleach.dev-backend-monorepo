package dev.jamesleach.neural.data

/**
 * Implementation of [DataPoint] without a label.
 */
data class UnlabeledDataPoint(
    private val inputData3d: Array<Array<DoubleArray>>,
    override val dataShape: DataShape,
) : DataPoint {
    override fun getInputData3d(height: Int, width: Int, depth: Int): Double {
        return inputData3d[height][width][depth]
    }
}