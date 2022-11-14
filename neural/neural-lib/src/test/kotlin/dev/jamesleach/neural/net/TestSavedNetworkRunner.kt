package dev.jamesleach.neural.net

import com.google.common.util.concurrent.UncheckedExecutionException
import dev.jamesleach.neural.data.DataShape
import dev.jamesleach.neural.data.UnlabeledDataPoint
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class TestSavedNetworkRunner {
    private val networkLoader: NetworkLoader = Mockito.mock<NetworkLoader>(NetworkLoader::class.java)
    private val savedNetworkRunner: SavedNetworkRunner = SavedNetworkRunner(networkLoader)

    @Test
    fun `Exception thrown when no network found`() {
        // Given
        `when`<WrappedNetwork>(networkLoader.load("id")).thenReturn(null)

        // When
        val exception = Assertions.assertThrows(
            UncheckedExecutionException::class.java
        ) {
            savedNetworkRunner.runClassification(
                "id",
                UnlabeledDataPoint(
                    Array(5) { Array(4) { DoubleArray(3) } },
                    DataShape(1, 1, 1, 1, 1)
                )
            )
        }

        // Then
        assertEquals("No network found for id 'id'", exception.cause!!.message)
    }
}