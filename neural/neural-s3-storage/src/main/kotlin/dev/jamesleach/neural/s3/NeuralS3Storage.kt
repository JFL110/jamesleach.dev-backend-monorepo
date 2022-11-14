package dev.jamesleach.neural.s3

import com.amazonaws.services.s3.AmazonS3
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.jamesleach.neural.data.DataShape
import dev.jamesleach.neural.net.NetworkLoader
import dev.jamesleach.neural.net.NetworkSaver
import dev.jamesleach.neural.net.NetworkSerializer
import dev.jamesleach.neural.net.WrappedNetwork
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream

private val log = LoggerFactory.getLogger(NeuralS3Storage::class.java)
private const val BUCKET_NAME = "saved-networks"

@Profile("!test")
@Primary
@Component
internal class NeuralS3Storage(
    private val objectMapper: ObjectMapper,
    private val serializer: NetworkSerializer,
    private val s3Client: AmazonS3,
) : NetworkLoader, NetworkSaver {

    override fun load(id: String): WrappedNetwork? {
        try {
            if (!s3Client.doesObjectExist(BUCKET_NAME, objectName("$id.meta"))) {
                return null
            }
            // Meta
            val metaObj = s3Client.getObject(BUCKET_NAME, objectName("$id.meta")) ?: return null
            val metaString = metaObj.objectContent.bufferedReader(Charsets.UTF_8).readText()
            val meta = objectMapper.readValue<MetaFormat>(metaString)

            // Network
            val network = s3Client.getObject(BUCKET_NAME, objectName(id))
            if (network == null) {
                log.warn("Found metadata but no network for network id {}", id)
                return null
            }
            val graph = serializer.deSerializeFrom(network.objectContent)
            return WrappedNetwork(meta.id, meta.shape, graph)

        } catch (e: Exception) {
            log.error("Error loading network from S3", e)
            return null
        }
    }

    /**
     * putObject uses UTF-8 encoding.
     */
    override fun save(network: WrappedNetwork) {
        log.info("Saving network '{}' in S3", network.id)
        val meta = MetaFormat(network.id, network.dataShape)
        val tempFile = File.createTempFile("network", network.id)
        FileOutputStream(tempFile, false).use { serializer.serializeTo(network.graph, it) }

        try {
            val json = objectMapper.writeValueAsString(meta)
            s3Client.putObject(BUCKET_NAME, objectName(network.id + ".meta"), json)
        } catch (e: Exception) {
            throw RuntimeException("Error saving network meta in S3", e)
        }

        try {
            s3Client.putObject(BUCKET_NAME, objectName(network.id), tempFile)
        } catch (e: Exception) {
            throw RuntimeException("Error saving network meta in S3", e)
        }

        tempFile.delete()
    }

    private data class MetaFormat(
        val id: String,
        val shape: DataShape
    )

    private fun objectName(id: String) = "$id.json"
}