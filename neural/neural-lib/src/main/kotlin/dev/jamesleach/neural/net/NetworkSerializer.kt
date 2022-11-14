package dev.jamesleach.neural.net

import org.deeplearning4j.nn.graph.ComputationGraph
import org.deeplearning4j.util.ModelSerializer
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*

private val CHARSET = StandardCharsets.UTF_8

/**
 * Serialize and Deserialize [ComputationGraph]s
 */
@Component
class NetworkSerializer {

    fun serializeTo(graph: ComputationGraph, outputStream: OutputStream) {
        ModelSerializer.writeModel(graph, outputStream, true)
    }

    fun deSerializeFrom(inputStream: InputStream): ComputationGraph {
        inputStream.use { return ModelSerializer.restoreComputationGraph(inputStream) }
    }

    /**
     * ComputationGraph -> Base64 String
     */
    fun serialize(graph: ComputationGraph): String {
        val bos = ByteArrayOutputStream()
        ModelSerializer.writeModel(graph, bos, true)
        return String(Base64.getEncoder().encode(bos.toByteArray()), CHARSET)
    }

    /**
     * Base64 String -> ComputationGraph
     */
    fun deserialize(base64: String): ComputationGraph {
        val bytes = Base64.getDecoder().decode(base64.toByteArray(CHARSET))
        val bis = ByteArrayInputStream(bytes)
        return ModelSerializer.restoreComputationGraph(bis)
    }
}