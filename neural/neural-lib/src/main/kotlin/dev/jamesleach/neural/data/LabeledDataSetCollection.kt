package dev.jamesleach.neural.data

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator

/**
 * Wrapper around a list of [LabeledDataSet]s.
 */
class LabeledDataSetCollection(_dataSets: Collection<LabeledDataSet>) {
    val dataSets: List<LabeledDataSet>
    val dataShape: DataShape?

    constructor(vararg dataSets: LabeledDataSet) : this(dataSets.toList())

    init {
        this.dataSets = _dataSets.toList()
        dataShape = if (this.dataSets.isEmpty()) null else this.dataSets[0].dataShape
    }

    /**
     * @return a [DataSetIterator] that iterates over all [LabeledDataSet].
     */
    fun toDataSetIterator(): DataSetIterator = ListDataSetIterator(dataSets.map { it.dataSet }.toList())
}