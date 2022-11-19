package dev.jamesleach.location.geotools

import org.locationtech.jts.geom.MultiPolygon

internal data class Country(
    val polygon: MultiPolygon,
    val name: String
)