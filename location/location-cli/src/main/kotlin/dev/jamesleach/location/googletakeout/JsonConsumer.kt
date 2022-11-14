package dev.jamesleach.location.googletakeout

import dev.jamesleach.location.Location
import java.io.InputStream

interface JsonConsumer {
    fun canConsume(name: String): Boolean
    fun consume(inputStream: InputStream): Sequence<Location>
}