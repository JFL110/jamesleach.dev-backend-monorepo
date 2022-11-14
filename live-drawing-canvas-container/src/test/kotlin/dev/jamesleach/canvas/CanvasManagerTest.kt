package dev.jamesleach.canvas

import com.google.common.collect.Maps
import dev.jamesleach.ZonedNowSupplier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import kotlin.streams.toList

class CanvasManagerTest {
    private val startingTime = ZonedDateTime.of(2019, 1, 1, 1, 1, 1, 1, ZoneId.of("UTC"))
    private val now = mock<ZonedNowSupplier>()
    private val canvasManager = CanvasManager(now)

    @Test
    fun `old canvases removed on cleanup`() {
        // Create a lot of canvases
        val canvasCreationTimes: MutableMap<String, ZonedDateTime> = Maps.newConcurrentMap()
        IntStream.range(0, 1500)
            .forEach { i: Int ->
                val canvasTime = startingTime.plusSeconds(i.toLong())
                val canvasId = UUID.randomUUID().toString()
                canvasCreationTimes[canvasId] = canvasTime
                `when`(now.get()).thenReturn(canvasTime)
                canvasManager.handleLineMessage(
                    canvasId,
                    { "some-user" },
                    LineMessage(
                        "s", 0, listOf(
                            Point(10.0, 10.0), Point(12.0, 12.0)
                        ), "#fff", 10, 0, true
                    )
                )
            }
        Assertions.assertEquals(1500, canvasManager.getCanvasCount())

        // Cleanup
        canvasManager.cleanupOldCanvases()
        Assertions.assertEquals(100, canvasManager.getCanvasCount())
        Assertions.assertEquals(1400, canvasManager.getRemovedCanvasCount())
        Assertions.assertEquals(1500, canvasManager.getTotalMessageCount())

        // Oldest canvases should have been removed
        val deletionCutoff = startingTime.plusSeconds(1400)
        canvasCreationTimes
            .forEach { (id: String?, time: ZonedDateTime) ->
                val numberOfCanvasLines = canvasManager.getAllLines(
                    id
                ) { "none" }.size

                // Canvases
                Assertions.assertEquals(
                    if (time.isBefore(deletionCutoff)) 0 else 1,
                    numberOfCanvasLines,
                    "time " + Duration.between(startingTime, time).seconds
                )
            }
    }

    @Test
    fun `messages ignored if canvas is filled`() {
        val canvasId = UUID.randomUUID().toString()
        `when`(now.get()).thenReturn(startingTime)
        IntStream.range(0, 1000500 / 3)
            .forEach { i: Int ->
                val response = canvasManager.handleLineMessage(
                    canvasId,
                    { "some-user" },
                    LineMessage(
                        "s", i, listOf(
                            Point(10.0, 10.0),
                            Point(12.0, 12.0)
                        ), "#fff", 10, 0, true
                    )
                )

                // Each line has two points so has a 'size' of 3
                if (i > 1000000 / 3) {
                    Assertions.assertNull(response)
                } else {
                    Assertions.assertNotNull(response)
                }
            }
        Assertions.assertEquals((1000500 / 3).toLong(), canvasManager.getTotalMessageCount())
    }

    /**
     * Do all operations a lot at the same time to (hopefully) surface any
     * concurrency exceptions
     */
    @Test
    fun testConcurrencyBomb() {
        val threads = Executors.newFixedThreadPool(10)
        var seconds = 0L
        `when`(now.get()).then { seconds++; startingTime.plusSeconds(seconds) }

        IntStream.range(0, 5000)
            .parallel()
            .mapToObj { i ->
                threads.submit {
                    val canvasId = "c- ${i % 150}"

                    canvasManager.handleLineMessage(
                        canvasId,  // More canvases than allowed so regular cleanup occurs
                        { "some-user" },
                        LineMessage(
                            if (i % 2 == 0) "s" else "c", i,
                            listOf(
                                Point(10.0, 10.0),
                                Point(12.0, 12.0)
                            ),
                            "#fff",
                            10,
                            0,
                            true
                        )
                    )
                    if (i % 100 == 0) {
                        canvasManager.cleanupOldCanvases()
                    }
                    if (i % 50 == 0) {
                        canvasManager.clearCanvas(canvasId)
                    }
                }
            }
            .toList()
            .forEach { f ->
                f[10, TimeUnit.SECONDS]
            }
        Assertions.assertEquals(5000, canvasManager.getTotalMessageCount())
        Assertions.assertTrue(canvasManager.getRemovedCanvasCount() > 0)
    }
}