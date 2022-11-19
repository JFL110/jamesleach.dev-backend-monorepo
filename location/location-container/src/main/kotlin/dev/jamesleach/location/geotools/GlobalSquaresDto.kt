package dev.jamesleach.location.geotools

data class GlobalSquaresDto(
    val totalCountrySquares: Long,
    val totalVisitedSquares: Long,
    val countries: List<CountrySquaresDto>
)

data class CountrySquaresDto(
    val countryName: String,
    val totalSquares: Long,
    val visitedSquares: Long
)