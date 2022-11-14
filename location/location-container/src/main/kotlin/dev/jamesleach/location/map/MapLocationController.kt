package dev.jamesleach.location.map

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MapLocationController(
    private val mapLocationDigester: MapLocationDigester
) {
    @PostMapping("/digest")
    fun digest() : String {
        mapLocationDigester.digestLocations()
        return "Digest finished"
    }
}