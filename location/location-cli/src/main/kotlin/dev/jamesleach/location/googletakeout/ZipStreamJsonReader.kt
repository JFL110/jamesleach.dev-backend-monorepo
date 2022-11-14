package dev.jamesleach.location.googletakeout

import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

@Component
class ZipStreamJsonReader {

    fun processJsonInZip(
        inputStream: InputStream,
        consumers: List<JsonConsumer>
    ) = sequence {
        ZipInputStream(inputStream)
            .use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".json")) {
                        consumers.find { it.canConsume(entry!!.name) }
                            ?.consume(InputStreamNoCloseWrapper(zipInputStream.buffered()))
                            ?.forEach { yield(it) }
                    }
                    entry = zipInputStream.nextEntry
                }
            }
    }

    /**
     * Wrapper around any InputStream that prevents it from being closed
     */
    internal class InputStreamNoCloseWrapper(
        private val inputStream: InputStream
    ) : InputStream() {
        override fun read() = inputStream.read()
        override fun readAllBytes(): ByteArray = inputStream.readAllBytes()
        override fun read(b: ByteArray) = inputStream.read(b)
        override fun available() = inputStream.available()
        override fun mark(readlimit: Int) = inputStream.mark(readlimit)
        override fun read(b: ByteArray, off: Int, len: Int) = inputStream.read(b, off, len)
        override fun skip(n: Long) = inputStream.skip(n)
        override fun readNBytes(len: Int): ByteArray = inputStream.readNBytes(len)
        override fun readNBytes(b: ByteArray?, off: Int, len: Int) = inputStream.readNBytes(b, off, len)
        override fun markSupported() = inputStream.markSupported()
        override fun reset() = inputStream.reset()
        override fun transferTo(out: OutputStream?) = inputStream.transferTo(out)
        // no close method
    }
}