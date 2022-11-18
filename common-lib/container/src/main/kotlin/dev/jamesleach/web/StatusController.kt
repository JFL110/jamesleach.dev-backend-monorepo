package dev.jamesleach.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

private const val DEFAULT_VERSION_FILE = "./version.json";
private val log = LoggerFactory.getLogger(StatusController::class.java)

/**
 * /ping endpoint for checking the app is up.
 */
@RestController
internal class StatusController(
    private val nowSupplier: ZonedNowSupplier,
    private val objectMapper: ObjectMapper,
    @Value("\${version-file:}") val versionFile: String?,
) {

    private val versionFileContents = lazy {
        val versionFileDefaulted = if (versionFile.isNullOrBlank()) DEFAULT_VERSION_FILE else versionFile!!
        val path = Paths.get(versionFileDefaulted)
        try {
            objectMapper.readValue<VersionDto>(path.toFile().readText(Charsets.UTF_8))
        } catch (e: FileNotFoundException) {
            log.error("No version file found - looked for ${path.toAbsolutePath()}")
            null
        }
    }

    @GetMapping("/status")
    fun status() = StatusDto(
        DurationFormatUtils.formatDurationHMS(Duration.between(startTime, nowSupplier.get()).toMillis()),
        versionFileContents.value
    )

    companion object {
        private val startTime = ZonedDateTime.now()
    }
}