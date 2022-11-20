package dev.jamesleach.location.geotools

import dev.jamesleach.location.square.BoundingBox
import dev.jamesleach.location.square.Point
import dev.jamesleach.location.square.SquareMapper
import org.geotools.geometry.jts.JTSFactoryFinder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate

class GlobalSquaresServiceTest {

    private val geometryFactory = JTSFactoryFinder.getGeometryFactory(null)
    private val squareMapper = SquareMapper()
    private val globalSquaresService = GlobalSquaresService(
        squareMapper,
        "../../globe-shapes/ne_50m_admin_0_countries.shp"
    )

    @Test
    fun `test example squares`() {
        val globalSquaresDto = globalSquaresService.buildGlobalSquaresDto(
            setOf(
                114345670,
                126584805,
                115172977,
                123111006
            )
        )

        assertEquals(53545696L, globalSquaresDto.totalCountrySquares)
        assertEquals(4, globalSquaresDto.totalVisitedSquares)
        assertEquals(242, globalSquaresDto.countries.size)

        val unitedKingdom = globalSquaresDto.countries.find { it.countryName == "United Kingdom" }!!
        assertEquals(82737, unitedKingdom.totalSquares)
        assertEquals(2, unitedKingdom.visitedSquares)

        val italy = globalSquaresDto.countries.find { it.countryName == "Italy" }!!
        assertEquals(82835, italy.totalSquares)
        assertEquals(1, italy.visitedSquares)

        val france = globalSquaresDto.countries.find { it.countryName == "France" }!!
        assertEquals(178731, france.totalSquares)
        assertEquals(1, france.visitedSquares)

        val zambia = globalSquaresDto.countries.find { it.countryName == "Zambia" }!!
        assertEquals(157217, zambia.totalSquares)
        assertEquals(0, zambia.visitedSquares)
    }

    @Test
    fun `validate area calculation`() {
        // Given
        val box = BoundingBox(
            Point(70.0, 60.0),
            Point(65.0, 57.0),
        )

        // When
        val area = box.toPolygon().area

        // Then
        assertEquals(15.0, area)
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