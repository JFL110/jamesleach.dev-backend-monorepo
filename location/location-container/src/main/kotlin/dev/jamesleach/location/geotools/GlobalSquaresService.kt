package dev.jamesleach.location.geotools

import dev.jamesleach.location.square.BoundingBox
import dev.jamesleach.location.square.Point
import dev.jamesleach.location.square.SquareMapper
import org.geotools.data.DataStoreFinder
import org.geotools.data.simple.SimpleFeatureIterator
import org.geotools.geometry.jts.JTSFactoryFinder
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.MultiPolygon
import org.opengis.feature.simple.SimpleFeature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Paths

private val log = LoggerFactory.getLogger(GlobalSquaresService::class.java)
private const val GLOBE_SHAPES_FILE =
    "./globe-shapes/ne_50m_admin_0_countries.shp"

@Component
class GlobalSquaresService(
    private val squareMapper: SquareMapper,
    @Value("\${global-shapes-file:}") val globalShapesFile: String?,

    ) {
    private val geometryFactory = JTSFactoryFinder.getGeometryFactory(null)

    fun buildGlobalSquaresDto(visitedSquares: Set<Long>): GlobalSquaresDto {
        val visitedSquaresByCountry = visitedSquaresByCountry(visitedSquares, countries.value)
        val countryTotals = calculateCountrySquaresTotal(countries.value)

        val countryCounts = countryTotals.entries.map {
            CountrySquaresDto(
                it.key.name,
                it.value,
                visitedSquaresByCountry[it.key.name] ?: 0
            )
        }
        return GlobalSquaresDto(
            countryCounts.sumOf { it.totalSquares },
            visitedSquares.size.toLong(),
            countryCounts
        )
    }

    private fun calculateCountrySquaresTotal(
        countries: List<Country>
    ): Map<Country, Long> =
        countries.associateWith {
            calculateCountrySquaresTotal(it)
        }

    private fun calculateCountrySquaresTotal(country: Country) =
        (country.polygon.area / (squareMapper.latitudeIncrement * squareMapper.longitudeIncrement)).toLong()

    private fun visitedSquaresByCountry(
        visitedSquares: Set<Long>,
        countries: List<Country>
    ): Map<String, Long> {
        val countrySquares = mutableMapOf<String, Long>()
        visitedSquares.forEach { square ->
            val boundingBox = squareMapper.toBoundingBox(square)
            val boundingBoxPolygon = boundingBox.toPolygon()
            countries.firstOrNull { it.polygon.intersects(boundingBoxPolygon) }
                ?.let { country ->
                    countrySquares.compute(country.name) { _, existing -> (existing ?: 0) + 1 }
                }
        }
        return countrySquares.toMap()
    }

    private val countries = lazy {
        val globalShapesFileDefaulted = if (globalShapesFile.isNullOrBlank()) GLOBE_SHAPES_FILE else globalShapesFile!!
        val connect = mapOf("url" to  Paths.get(globalShapesFileDefaulted).toUri().toString())

        val dataStore = DataStoreFinder.getDataStore(connect)
        val typeName = dataStore.typeNames[0]
        log.info("Reading content $typeName")

        val featureSource = dataStore.getFeatureSource(typeName)
        featureSource.features.features().asSequence().map { feature ->
            Country(
                feature.defaultGeometry as MultiPolygon,
                feature.getAttribute("NAME") as String
            )
        }.toList()
    }

    private fun SimpleFeatureIterator.asSequence(): Sequence<SimpleFeature> {
        val iterator = this
        return sequence {
            iterator.use { iterator ->
                while (iterator.hasNext()) {
                    yield(iterator.next())
                }
            }
        }
    }

    private fun Point.toCoordinate() = Coordinate(this.longitude, this.latitude)

    private fun BoundingBox.toPolygon() = geometryFactory.createPolygon(
        arrayOf(
            this.northWest.toCoordinate(),
            this.southWest.toCoordinate(),
            this.southEast.toCoordinate(),
            this.northEast.toCoordinate(),
            this.northWest.toCoordinate(),
        )
    )
}