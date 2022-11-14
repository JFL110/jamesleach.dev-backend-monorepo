package dev.jamesleach.neural.data

import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterators
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestDataSetDtos {

    @Test
    fun `Labeled data sets with single depth`() {
        // Labeled data point
        val data = arrayOf(
            arrayOf(doubleArrayOf(1.0), doubleArrayOf(2.0), doubleArrayOf(3.0)),
            arrayOf(doubleArrayOf(4.0), doubleArrayOf(5.0), doubleArrayOf(6.0))
        )
        val label = doubleArrayOf(0.0, 0.0, 1.0, 0.0)
        val point = LabeledDataPoint(data, label)
        assertEquals(data, point.inputData3d)
        assertEquals(label, point.labels)
        assertEquals(3, point.dataShape.numDimensions)
        assertEquals(2, point.dataShape.height)
        assertEquals(3, point.dataShape.length)
        assertEquals(1, point.dataShape.depth)
        assertEquals(4, point.dataShape.numLabels)

        // Data set - single point
        var dataSet = LabeledDataSet(ImmutableList.of(point))
        assertEquals(point.dataShape, dataSet.dataShape)
        var features = dataSet.dataSet.features
        var labels = dataSet.dataSet.labels
        assertEquals(6, features.length())
        assertArrayEquals(longArrayOf(1, 1, 2, 3), features.shape())
        assertEquals(4, labels.length())
        assertArrayEquals(longArrayOf(1, 4), labels.shape())

        // Data set - multi point
        dataSet = LabeledDataSet(ImmutableList.of(point, point))
        assertEquals(point.dataShape, dataSet.dataShape)
        features = dataSet.dataSet.features
        labels = dataSet.dataSet.labels
        assertEquals(12, features.length())
        assertArrayEquals(longArrayOf(2, 1, 2, 3), features.shape())
        assertEquals(8, labels.length())
        assertArrayEquals(longArrayOf(2, 4), labels.shape())

        // Singleton iterator
        assertEquals(dataSet.dataSet, Iterators.getOnlyElement(dataSet.toSingletonIterator()))
    }

    @Test
    fun `Labeled data points with dual depth`() {
        val data = arrayOf(
            arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0), doubleArrayOf(5.0, 6.0)),
            arrayOf(doubleArrayOf(7.0, 8.0), doubleArrayOf(9.0, 10.0), doubleArrayOf(11.0, 12.0))
        )
        val label = doubleArrayOf(0.0, 1.0)
        val point = LabeledDataPoint(data, label)
        assertEquals(data, point.inputData3d)
        assertEquals(label, point.labels)
        assertEquals(3, point.dataShape.numDimensions)
        assertEquals(2, point.dataShape.height)
        assertEquals(3, point.dataShape.length)
        assertEquals(2, point.dataShape.depth)
        assertEquals(2, point.dataShape.numLabels)

        // Data set - single point
        var dataSet = LabeledDataSet(ImmutableList.of(point))
        assertEquals(point.dataShape, dataSet.dataShape)
        var features = dataSet.dataSet.features
        var labels = dataSet.dataSet.labels
        assertEquals(12, features.length())
        assertArrayEquals(longArrayOf(1, 2, 2, 3), features.shape())
        assertEquals(2, labels.length())
        assertArrayEquals(longArrayOf(1, 2), labels.shape())

        // Data set - multi point
        dataSet = LabeledDataSet(ImmutableList.of(point, point))
        assertEquals(point.dataShape, dataSet.dataShape)
        features = dataSet.dataSet.features
        labels = dataSet.dataSet.labels
        assertEquals(24, features.length())
        assertArrayEquals(longArrayOf(2, 2, 2, 3), features.shape())
        assertEquals(4, labels.length())
        assertArrayEquals(longArrayOf(2, 2), labels.shape())

        // Data set collection - single
        val dataSetCollection = LabeledDataSetCollection(dataSet)
        assertEquals(point.dataShape, dataSetCollection.dataShape)
        assertEquals(dataSetCollection.dataSets.size, 1)
        assertThat(dataSetCollection.dataSets, hasItem(dataSet))
        assertEquals(dataSet.dataSet, Iterators.getOnlyElement(dataSetCollection.toDataSetIterator()))
    }

    @Test
    fun `Cannot create empty data set`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            LabeledDataSet(
                ImmutableList.of()
            )
        }
    }
}