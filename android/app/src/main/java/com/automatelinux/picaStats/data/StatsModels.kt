package com.automatelinux.picaStats.data

// Mirrors the JSON returned by the picaStats backend /api/stats route.
data class StatsResponse(
    val app: String = "",
    val site: String = "",
    val generatedAt: String = "",
    val summary: Summary = Summary(),
    val byDay: List<DayPoint> = emptyList(),
    val topReferrers: List<Referrer> = emptyList(),
    val countries: List<Country> = emptyList(),
    val wishes: Int? = null,
    val allSites: List<SiteRow> = emptyList(),
)

data class Summary(
    val today: Int = 0,
    val yesterday: Int = 0,
    val last7d: Int = 0,
    val last30d: Int = 0,
    val allTime: Int = 0,
    val uniqueAllTime: Int = 0,
    val humans: Int = 0,
    val bots: Int = 0,
)

data class DayPoint(
    val date: String = "",
    val visits: Int = 0,
    val unique: Int = 0,
)

data class Referrer(
    val referer: String = "",
    val visits: Int = 0,
)

data class Country(
    val name: String = "",
    val code: String = "",
    val visits: Int = 0,
    val visitors: Int = 0,
)

data class SiteRow(
    val site: String = "",
    val visits: Int = 0,
    val unique: Int = 0,
)
