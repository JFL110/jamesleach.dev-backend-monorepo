package dev.jamesleach.canvas

import com.google.common.collect.Maps
import dev.jamesleach.ZonedNowSupplier
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.security.Principal
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Service to hold (in memory) and apply changes to canvases.
 */
// Apply some very basic limits on size and number of canvases
private const val MAX_SIZE_PER_CANVAS: Long = 1000000
private const val MAX_CANVASES = 100
private const val CLEANUP_CANVASES_TASK_INTERVAL_MS = (3 * 60 * 1000).toLong()

// Message types
private const val START_LINE_TYPE = "s"
private const val CONTINUE_LINE_TYPE = "c"
private const val FINISH_LINE_TYPE = "f"

@Component
class CanvasManager(
    private val now: ZonedNowSupplier
) {
    private val log = LoggerFactory.getLogger(CanvasManager::class.java)

    // State
    private val cleanupLock: Lock = ReentrantLock()
    private val canvases: MutableMap<String, Canvas> = Maps.newConcurrentMap()
    private val clearedCanvasesCount: AtomicLong = AtomicLong()
    private val totalMessageCount: AtomicLong = AtomicLong()
    private val removedCanvasCount: AtomicLong = AtomicLong()

    /**
     * Fetch or create a canvas
     */
    private fun getCanvas(canvasId: String): Canvas {
        return canvases.computeIfAbsent(canvasId) { Canvas(canvasId, now.get()) }
    }

    @Scheduled(fixedDelay = CLEANUP_CANVASES_TASK_INTERVAL_MS)
    fun cleanupOldCanvases() {
        log.debug("Scheduled canvas cleanup.")
        cleanupLock.lock()
        try {
            val canvasesToRemove = canvases.size - MAX_CANVASES
            if (canvasesToRemove <= 0) {
                return
            }
            log.info("Removing {} canvases.", canvasesToRemove)

            // Remove that many canvases, oldest first
            canvases.values
                .asSequence()
                .map { c -> CanvasWithFixedUpdateTime(c, c.lastUpdateTime.get()) }
                .toList() // Collect and fix all update times
                .sortedBy { a -> a.lastUpdateTime } // Oldest first
                .take(canvasesToRemove)
                .map { c -> c.canvas.id }
                .toList()
                .forEach { i ->
                    canvases.remove(i)
                    removedCanvasCount.incrementAndGet()
                }
        } catch (e: Exception) {
            log.error("Error cleaning up old canvases.", e)
            throw e
        } finally {
            cleanupLock.unlock()
        }
    }

    /**
     * Completely clear a canvas
     */
    fun clearCanvas(id: String) {
        try {
            canvases.remove(id)
            clearedCanvasesCount.incrementAndGet()
        } catch (e: Exception) {
            log.error("Error clearing canvas ${id} up old canvases.", e)
            throw e
        }
    }

    /**
     * List all the lines in a canvas
     */
    fun getAllLines(canvasId: String, excludingUser: Principal) =
        getCanvas(canvasId).lines.values
            .filter { l: Line -> l.userId != excludingUser.name }
            .toList()

    /**
     * Create or continue a line in a canvas
     */
    fun handleLineMessage(canvasId: String, principal: Principal, msg: LineMessage): LineMessageOut? {
        totalMessageCount.incrementAndGet()
        val key = LineKey(principal.name, msg.clientLineNumber)
        val canvas = getCanvas(canvasId)

        // Limit canvas size
        if (canvas.sizeEstimation.get() > MAX_SIZE_PER_CANVAS) {
            log.debug("Canvas is full [{}] - ignoring message", canvasId)
            return null
        }

        // Update the last time this canvas was touched
        canvas.lastUpdateTime.set(now.get())
        if (START_LINE_TYPE == msg.type) {
            canvas.lines[key] = Line(
                key.ownerId,
                key.ownerLineNumber,
                canvas.zIndex.incrementAndGet(),
                msg.points?.filter { Objects.nonNull(it) }?.toMutableList() ?: mutableListOf(),
                msg.brushRadius,
                msg.brushColor,
                msg.isFinished
            )
            canvas.sizeEstimation.addAndGet(1 + (msg.points?.size?.toLong() ?: 0))
        }

        var line: Line = canvas.lines[key]
            ?: // Line create message was lost or out of sequence
            return null
        if (CONTINUE_LINE_TYPE == msg.type) {
            msg.points?.stream()?.filter { obj -> Objects.nonNull(obj) }?.forEach(line.points::add)
            canvas.sizeEstimation.addAndGet(msg.points?.size?.toLong() ?: 0)
        } else if (FINISH_LINE_TYPE == msg.type) {
            line = line.copy(finished = true)
            canvas.lines[key] = line
        }
        return LineMessageOut(
            principal.name,
            msg.type,
            msg.clientLineNumber,
            msg.points,
            msg.brushColor,
            msg.brushRadius,
            msg.pointsIndexStart,
            line.finished
        )
    }

    /**
     * A Canvas.
     */
    private data class Canvas (val id: String, val creationTime: ZonedDateTime) {
        val sizeEstimation: AtomicLong = AtomicLong()
        val lines: MutableMap<LineKey, Line> = Maps.newConcurrentMap()
        val zIndex: AtomicInteger = AtomicInteger()
        val lastUpdateTime: AtomicReference<ZonedDateTime> = AtomicReference<ZonedDateTime>(creationTime)
    }

    /**
     * Wrap a canvas with its last update time fixed so it won't be modified while
     * sorting canvases for deletion
     */
    private data class CanvasWithFixedUpdateTime(
        val canvas: Canvas,
        val lastUpdateTime: ZonedDateTime,
    )

    /**
     * Hash key for a Line
     */
    private data class LineKey(
        val ownerId: String,
        val ownerLineNumber: Int
    )

    // State reporting
    fun getCanvasCount() = canvases.size

    fun getRemovedCanvasCount() = removedCanvasCount.get()

    fun getTotalMessageCount() = totalMessageCount.get()

    fun getClearedCanvasCount() = clearedCanvasesCount.get()
}