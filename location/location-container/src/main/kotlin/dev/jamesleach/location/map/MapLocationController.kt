package dev.jamesleach.location.map

import dev.jamesleach.web.BadRequestException
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MapLocationController(
    private val mapLocationDigester: MapLocationDigester,
    @Value("\${digest.securityKey}") val securityKey: String,
) {
    @PostMapping("/digest")
    fun digest(@RequestBody requestSecurityKey: String?): String {
        if (securityKey != requestSecurityKey) {
            throw BadRequestException("Invalid security key $requestSecurityKey")
        }
        mapLocationDigester.digestLocations()
        return "Digest finished"
    }
}