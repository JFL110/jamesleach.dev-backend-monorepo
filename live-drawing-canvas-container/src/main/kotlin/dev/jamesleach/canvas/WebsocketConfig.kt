package dev.jamesleach.canvas

import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.*

/**
 * Configuration for the websocket.
 *
 * @author jim
 */
@Configuration
@EnableWebSocketMessageBroker
internal class WebsocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val handshaker = object : DefaultHandshakeHandler() {
            override fun determineUser(
                request: ServerHttpRequest,
                wsHandler: WebSocketHandler,
                attributes: Map<String?, Any?>
            ): Principal = SimpleUser(UUID.randomUUID().toString())
        }

        registry.addEndpoint("/canvas")
            .setAllowedOriginPatterns("http://*", "https://*")
            .setHandshakeHandler(handshaker)
        registry.addEndpoint("/canvas")
            .setAllowedOriginPatterns("http://*", "https://*")
            .setHandshakeHandler(handshaker)
            .withSockJS()
    }

    private data class SimpleUser(
        private val id: String
    ) : Principal {
        override fun getName() = id
    }
}