package dev.jamesleach.neural.data

interface DataPoint {
    /**
     * Network input in the form [height][width][depth]
     */
    fun getInputData3d(height: Int, width: Int, depth: Int): Double

    /**
     * The shape of the network input data.
     */
    val dataShape: DataShape
}