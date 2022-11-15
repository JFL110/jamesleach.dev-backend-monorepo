package dev.jamesleach.web

import dev.jamesleach.ZonedNowSupplier
import org.apache.commons.lang3.time.DurationFormatUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.ZonedDateTime

private val startTime = ZonedDateTime.now()

/**
 * /ping endpoint for checking the app is up.
 */
@RestController
internal class StatusController(
    private val nowSupplier: ZonedNowSupplier
) {
    @GetMapping("/status")
    fun status() = StatusDto(
        DurationFormatUtils.formatDurationHMS(Duration.between(startTime, nowSupplier.get()).toMillis())
    )
}