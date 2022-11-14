package dev.jamesleach.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * /ping endpoint for checking the app is up.
 */
@RestController
internal class PingController {
    @GetMapping("/ping")
    fun ping() = "pong"
}