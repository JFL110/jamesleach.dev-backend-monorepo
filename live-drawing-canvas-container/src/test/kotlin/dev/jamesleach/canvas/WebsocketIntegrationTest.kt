package dev.jamesleach.canvas

import com.google.common.collect.Lists
import dev.jamesleach.ObjectMapperConfiguration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val USER_INIT_CHANNEL = "/user/topic/init/"
private const val USER_INIT_DEST = "/app/canvas/init/"
private const val CANVAS_CHANNEL = "/topic/canvas/"
private const val CANVAS_DEST = "/app/canvas/line/"
private const val CLEAR_CHANNEL = "/topic/clear/"
private const val CLEAR_DEST = "/app/canvas/clear/"

/**
 * Mini end-to-end integration test that creates a websocket and tests
 * interactions with a couple of clients.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebsocketIntegrationTest {

    @LocalServerPort
    private val port = 0

    @Test
    fun `canvas flow`() {
        // Given
        val canvasId = UUID.randomUUID().toString()

        // Connect new client to empty canvas
        val c1 = CanvasClient(canvasId).connectAndSubscribe(url("/canvas"))

        // Assert canvas is empty
        assertEquals(1, c1.initMessageHandler.receivedObjects.size)
        assertTrue(c1.initMessage!!.lines.isEmpty())

        // Send a start line message
        c1.lineMessageHandler.setObjectCountToWaitFor(1)
        c1.sendLineMessage(
            LineMessage(
                "s", 0, listOf(
                    Point(10.0, 10.0),
                    Point(12.0, 11.0)
                ), "#fff", 10, 0, false
            )
        )

        // Wait for and verify message received back
        c1.lineMessageHandler.waitForMessages()
        var msg = c1.lineMessageHandler.receivedObjects[0]
        assertEquals(c1.initMessage!!.userId, msg.clientId)
        assertEquals(0, msg.clientLineNumber)
        assertEquals(10, msg.brushRadius)
        assertEquals("#fff", msg.brushColor)
        assertFalse(msg.isFinished ?: false)
        assertEquals("s", msg.type)
        assertEquals(2, msg.pointsNonNull().size)
        assertEquals(10.0, msg.pointsNonNull()[0].x)
        assertEquals(10.0, msg.pointsNonNull()[0].y)
        assertEquals(12.0, msg.pointsNonNull()[1].x)
        assertEquals(11.0, msg.pointsNonNull()[1].y)

        // Send a continuation of the line
        c1.lineMessageHandler.setObjectCountToWaitFor(1)
        c1.sendLineMessage(
            LineMessage(
                "c", 0, listOf(
                    Point(13.0, 12.0)
                ), "#fff", 8, 0, false
            )
        )
        c1.lineMessageHandler.waitForMessages()
        msg = c1.lineMessageHandler.receivedObjects[1]
        assertEquals(c1.initMessage!!.userId, msg.clientId)
        assertEquals(0, msg.clientLineNumber)
        assertFalse(msg.isFinished ?: false)
        assertEquals("c", msg.type)
        assertEquals(1, msg.pointsNonNull().size)
        assertEquals(13.0, msg.pointsNonNull()[0].x)
        assertEquals(12.0, msg.pointsNonNull()[0].y)

        // Send a finished line message
        c1.lineMessageHandler.setObjectCountToWaitFor(1)
        c1.sendLineMessage(LineMessage("f", 0, listOf(), "#fff", 8, 0, false))
        c1.lineMessageHandler.waitForMessages()
        msg = c1.lineMessageHandler.receivedObjects[2]
        assertEquals(c1.initMessage!!.userId, msg.clientId)
        assertEquals(0, msg.clientLineNumber)
        assertTrue(msg.isFinished ?: false)
        assertEquals("f", msg.type)

        // Connect new client to canvas
        val c2 = CanvasClient(canvasId).connectAndSubscribe(url("/canvas"))

        // New client as different id
        assertNotEquals(c1.initMessage!!.userId, c2.initMessage!!.userId)

        // Canvas has expected line
        assertEquals(1, c2.initMessage!!.lines.size)
        val (userId, ownerLineNumber, zIndex, points, brushRadius, brushColor) = c2.initMessage!!.lines[0]
        assertEquals(c1.initMessage!!.userId, userId)
        assertEquals(0, ownerLineNumber)
        assertEquals(10, brushRadius)
        assertEquals("#fff", brushColor)
        assertEquals(1, zIndex)
        assertEquals(3, points.size)
        assertEquals(10.0, points[0].x)
        assertEquals(10.0, points[0].y)
        assertEquals(12.0, points[1].x)
        assertEquals(11.0, points[1].y)
        assertEquals(13.0, points[2].x)
        assertEquals(12.0, points[2].y)

        // Second client adds a short line
        c1.lineMessageHandler.setObjectCountToWaitFor(1)
        c2.lineMessageHandler.setObjectCountToWaitFor(1)
        c1.sendLineMessage(
            LineMessage(
                "s",
                1,
                listOf(Point(15.0, 13.0), Point(15.0, 12.0)),
                "#ccc",
                8,
                0,
                true
            )
        )

        // Both clients get the same message
        c1.lineMessageHandler.waitForMessages()
        c2.lineMessageHandler.waitForMessages()
        val c1Message = c1.lineMessageHandler.receivedObjects[3]
        val c2Message = c2.lineMessageHandler.receivedObjects[0]
        listOf(c1Message, c2Message)
            .forEach { (clientId, type, clientLineNumber, points1, brushColor1, brushRadius1, _, isFinished): LineMessageOut ->
                assertNotNull(points1!!)
                assertEquals(c1.initMessage!!.userId, clientId)
                assertEquals(1, clientLineNumber)
                assertTrue(isFinished ?: false)
                assertEquals("s", type)
                assertEquals(2, points1.size)
                assertEquals(8, brushRadius1)
                assertEquals("#ccc", brushColor1)
                assertEquals(15.0, points1[0].x)
                assertEquals(13.0, points1[0].y)
                assertEquals(15.0, points1[1].x)
                assertEquals(12.0, points1[1].y)
            }

        // Get the app status endpoint response
        var statusResponse = RestTemplate().getForObject("http://localhost:$port", String::class.java)
        assertEquals(
            "App status ok. [2] connected users. [1] canvases. [0] canvases removed. [0] canvases cleared. [4] total messages.",
            statusResponse
        )

        // Clear a canvas
        c1.clearMessageHandler.setObjectCountToWaitFor(1)
        c2.clearMessageHandler.setObjectCountToWaitFor(1)
        c1.sendClearMessage()
        c1.clearMessageHandler.waitForMessages()
        c2.clearMessageHandler.waitForMessages()
        assertEquals("cleared", c1.clearMessageHandler.receivedObjects[0].text)
        assertEquals("cleared", c2.clearMessageHandler.receivedObjects[0].text)

        // Get the app status endpoint response again
        statusResponse = RestTemplate().getForObject("http://localhost:$port", String::class.java)
        assertEquals(
            "App status ok. [2] connected users. [0] canvases. [0] canvases removed. [1] canvases cleared. [4] total messages.",
            statusResponse
        )
    }

    private fun url(suffix: String): String {
        return "ws://localhost:$port$suffix"
    }

    /**
     * Testing client
     */
    private class CanvasClient(val canvasId: String) {

        val lineMessageHandler = TestingFrameHandler(LineMessageOut::class.java)
        val initMessageHandler = TestingFrameHandler(InitialCanvasMessage::class.java)
        val clearMessageHandler = TestingFrameHandler(ClearedCanvasMessageOut::class.java)
        var stompSession: StompSession? = null
        var initMessage: InitialCanvasMessage? = null

        fun connectAndSubscribe(url: String): CanvasClient {
            // Connect
            val stompClient = WebSocketStompClient(
                SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
            )
            val converter = MappingJackson2MessageConverter()
            converter.objectMapper = ObjectMapperConfiguration().objectMapper()
            stompClient.messageConverter = converter

            stompSession = stompClient.connect(url, object : StompSessionHandlerAdapter() {
                override fun handleException(
                    session: StompSession,
                    @Nullable command: StompCommand?,
                    headers: StompHeaders,
                    payload: ByteArray,
                    exception: Throwable
                ) {
                    throw RuntimeException("Failure in WebSocket handling", exception)
                }
            })[1, TimeUnit.SECONDS]
            println(stompSession!!.sessionId)

            // Subscribe
            stompSession!!.subscribe(CANVAS_CHANNEL + canvasId, lineMessageHandler)
            stompSession!!.subscribe(USER_INIT_CHANNEL + canvasId, initMessageHandler)
            stompSession!!.subscribe(CLEAR_CHANNEL + canvasId, clearMessageHandler)

            // Send init message and wait
            initMessageHandler.setObjectCountToWaitFor(1)
            stompSession!!.send(USER_INIT_DEST + canvasId, "{}")
            if (!initMessageHandler.objectWaitLatch!!.await(1, TimeUnit.SECONDS)) {
                fail<Any>("Gave up waiting for init message")
            }

            // Process initialisation message
            assertEquals(1, initMessageHandler.receivedObjects.size)
            val initMessage = initMessageHandler.receivedObjects[0]
            assertNotNull(initMessage)
            this.initMessage = initMessage
            return this
        }

        fun sendLineMessage(msg: LineMessage) {
            stompSession!!.send(CANVAS_DEST + canvasId, msg)
        }

        fun sendClearMessage() {
            stompSession!!.send(CLEAR_DEST + canvasId, "")
        }
    }

    /**
     * Stomp frame handler that captures received objects
     */
    private class TestingFrameHandler<T>(val type: Class<T>) : StompFrameHandler {

        var objectWaitLatch: CountDownLatch? = null
        val receivedObjects: MutableList<T> = Lists.newCopyOnWriteArrayList()

        @NonNull
        override fun getPayloadType(@NonNull headers: StompHeaders): Type {
            return type
        }

        fun setObjectCountToWaitFor(count: Int) {
            objectWaitLatch = CountDownLatch(count)
        }

        fun waitForMessages() {
            if (!objectWaitLatch!!.await(1, TimeUnit.SECONDS)) {
                fail<Any>("Gave up waiting for messages")
            }
        }

        override fun handleFrame(headers: StompHeaders, @Nullable payload: Any?) {
            println("Message: $payload")
            receivedObjects.add(payload as T)
            if (objectWaitLatch != null) {
                objectWaitLatch!!.countDown()
            }
        }
    }
}