package dev.jamesleach.neural.net

/**
 * Load saved networks
 */
interface NetworkLoader {
    fun load(id: String): WrappedNetwork?
}