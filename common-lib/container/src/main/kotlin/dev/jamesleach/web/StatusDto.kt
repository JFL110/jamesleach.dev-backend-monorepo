package dev.jamesleach.web

data class StatusDto(
    val uptime: String,
    val version: VersionDto?
)
