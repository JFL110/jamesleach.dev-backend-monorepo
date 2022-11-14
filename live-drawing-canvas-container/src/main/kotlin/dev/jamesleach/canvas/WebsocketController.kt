package dev.jamesleach.canvas

import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

/**
 * Endpoint methods for the websocket.
 */
@Controller
internal class WebsocketController(
    val canvasManager: CanvasManager
) {
    private val log = LoggerFactory.getLogger(WebsocketController::class.java)

    @MessageMapping("/canvas/line/{canvasId}")
    @SendTo("/topic/canvas/{canvasId}")
    fun line(
        @DestinationVariable("canvasId") canvasId: String,
        message: LineMessage,
        principal: Principal
    ): LineMessageOut? {
        log.debug("Got {}", message)
        return canvasManager.handleLineMessage(canvasId, principal, message)
    }

    @MessageMapping("/canvas/clear/{canvasId}")
    @SendTo("/topic/clear/{canvasId}")
    fun clear(@DestinationVariable("canvasId") canvasId: String, principal: Principal): ClearedCanvasMessageOut {
        log.info("Got clear {}", canvasId)
        canvasManager.clearCanvas(canvasId)
        return ClearedCanvasMessageOut("cleared")
    }

    @MessageMapping("/canvas/init/{canvasId}")
    @SendToUser("/topic/init/{canvasId}")
    fun initalCanvas(@DestinationVariable("canvasId") canvasId: String, principal: Principal): InitialCanvasMessage {
        log.debug("Got initial canvas {} request from {}", canvasId, principal.name)
        return InitialCanvasMessage(principal.name, canvasManager.getAllLines(canvasId, principal))
    }
}