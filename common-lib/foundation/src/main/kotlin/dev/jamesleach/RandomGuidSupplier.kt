package dev.jamesleach

import org.springframework.stereotype.Component
import java.util.*
import java.util.function.Supplier

/**
 * Generate random UUIDs
 */
@Component
class RandomGuidSupplier : Supplier<String> {
    override fun get() = UUID.randomUUID().toString()
}