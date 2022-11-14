package dev.jamesleach.neural.net

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import dev.jamesleach.neural.data.DataShape
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger(LocalFileNetworkSaverLoader::class.java)
private val CHARSET = StandardCharsets.UTF_8
private const val DEFAULT_DIR = "./saved-networks"

/**
 * Local file system implementation of NetworkLoader and NetworkSaver
 */
@Component
class LocalFileNetworkSaverLoader(
    @Value("\${saved-networks-path:null}")
    private val savedNetworksPath: String?,
    private val serializer: NetworkSerializer,
    private val objectMapper: ObjectMapper,
) : NetworkLoader, NetworkSaver {

    private fun dir() = Paths.get(savedNetworksPath ?: DEFAULT_DIR)

    override fun load(id: String): WrappedNetwork? {
        val stopwatch = Stopwatch.createStarted()
        log.info("Loading network '{}'", id)

        initDir()
        val files = files(id)
        if (!files.metaFile.toFile().exists()) {
            log.info("No file '{}'", files.metaFile)
            return null
        }
        if (!files.networkFile.toFile().exists()) {
            log.info("No file '{}'", files.networkFile)
            return null
        }
        val json = Files.readString(files.metaFile)
        val meta = objectMapper.readValue(json, MetaFormat::class.java)
        files.networkFile.toFile().inputStream().use { fileInputStream ->
            val output =
                WrappedNetwork(
                    meta.id,
                    meta.shape,
                    serializer.deSerializeFrom(fileInputStream)
                )
            log.info("Loaded network in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))
            return output
        }
    }

    override fun save(network: WrappedNetwork) {
        val stopwatch = Stopwatch.createStarted()
        log.info("Saving network '{}'", network.id)
        initDir()
        val meta = MetaFormat(network.id, network.dataShape)
        val metaJson = objectMapper.writeValueAsString(meta)
        val files = files(network.id)

        // Create empty temp file
        val base64File = files.inProgressNetworkFile.toFile()
        FileUtils.touch(base64File)
        Files.write(files.inProgressNetworkFile, ByteArray(0), StandardOpenOption.TRUNCATE_EXISTING)
        FileOutputStream(base64File, false).use { fileOutputStream ->
            serializer.serializeTo(network.graph, fileOutputStream)
        }

        // Move temp file to actual location
        FileUtils.deleteQuietly(files.networkFile.toFile())
        FileUtils.moveFile(base64File, files.networkFile.toFile())

        // Write meta file
        Files.write(files.metaFile, metaJson.toByteArray(CHARSET))
        log.info("Saved network '{}' in {}ms", network.id, stopwatch.elapsed(TimeUnit.MILLISECONDS))
    }

    private fun files(id: String): FilesTuple {
        return FilesTuple(
            dir().resolve("$id.netmeta$JSON_VERSION.json"),
            dir().resolve("$id.net$JSON_VERSION.bin"),
            dir().resolve("$id.net$JSON_VERSION.bin.part")
        )
    }

    private fun initDir() =
        check(!(!dir().toFile().exists() && !dir().toFile().mkdir())) { "Could not create working directory " + dir() }

    internal data class FilesTuple(
        val metaFile: Path,
        val networkFile: Path,
        val inProgressNetworkFile: Path,
    )

    internal data class MetaFormat(
        val id: String,
        val shape: DataShape,
    )
}