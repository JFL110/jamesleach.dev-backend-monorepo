package dev.jamesleach.canvas

import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint to return the status of the app.
 */
@RestController
class AppStatusController(
    private val simpUserRegistry: SimpUserRegistry,
    private val canvasManager: CanvasManager,
) {
    @RequestMapping("/")
    fun index() = """
        App status ok. [${simpUserRegistry.userCount}] connected users. 
        [${canvasManager.getCanvasCount()}] canvases. 
        [${canvasManager.getRemovedCanvasCount()}] canvases removed. 
        [${canvasManager.getClearedCanvasCount()}] canvases cleared. 
        [${canvasManager.getTotalMessageCount()}] total messages.
    """.trimIndent().replace("\n", "")
}