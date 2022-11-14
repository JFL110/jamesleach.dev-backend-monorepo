package dev.jamesleach

import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.function.Supplier

/**
 * Inject the current time
 */
@Component
class ZonedNowSupplier : Supplier<ZonedDateTime> {
    override fun get(): ZonedDateTime = ZonedDateTime.now()
}