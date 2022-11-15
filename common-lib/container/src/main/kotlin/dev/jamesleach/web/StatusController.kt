package dev.jamesleach.web

import dev.jamesleach.ZonedNowSupplier
import org.apache.commons.lang3.time.DurationFormatUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.FileNotFoundException
import java.nio.file.Paths
import java.time.Duration
import java.time.ZonedDateTime

private val startTime = ZonedDateTime.now()
private const val DEFAULT_VERSION_FILE = "./status.txt";
private val log = LoggerFactory.getLogger(StatusController::class.java)

/**
 * /ping endpoint for checking the app is up.
 */
@RestController
internal class StatusController(
    private val nowSupplier: ZonedNowSupplier,
    @Value("\${version-file:}") val versionFile: String?,
) {

    private val versionFileContents = lazy {
        val path =  Paths.get(versionFile ?: DEFAULT_VERSION_FILE)
        try {
            path.toFile().readText(Charsets.UTF_8)
        }catch (e: FileNotFoundException){
            log.error("No version file found - looked for ${path.toAbsolutePath()}")
            throw e
        }
    }

    @GetMapping("/status")
    fun status() = StatusDto(
        DurationFormatUtils.formatDurationHMS(Duration.between(startTime, nowSupplier.get()).toMillis()),
        versionFileContents.value
    )
}