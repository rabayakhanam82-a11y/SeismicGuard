package com.example.data.model

data class DisasterNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val category: String, // "SEISMIC", "WEATHER", "TSUNAMI", "BMD_BULLETIN"
    val timestampFormatted: String,
    val severityTag: String, // "ADVISORY", "WATCH", "WARNING", "INFO"
    val sourceAgency: String // "USGS Geological Survey", "BMD Agargaon", "EMSC Euro-Med", "BMD Cyclone Warning"
)

object DefaultDisasterNews {
    val sampleNews = listOf(
        DisasterNewsItem(
            id = "news_1",
            title = "BMD Special Weather Bulletin: Active Monsoon Axis Over Sylhet & Chittagong",
            summary = "Bangladesh Meteorological Department notes moderate squally weather along Bay of Bengal maritime ports. Landslide alert maintained for southeastern hill tracts.",
            category = "BMD_BULLETIN",
            timestampFormatted = "24m ago",
            severityTag = "ADVISORY",
            sourceAgency = "BMD Agargaon"
        ),
        DisasterNewsItem(
            id = "news_2",
            title = "Dauki & Madhupur Fault Strain Release Micro-Tremors Observed",
            summary = "Geological seismic array in northeastern corridor logs low-amplitude 2.1 M depth vibrations. No structural integrity risks reported.",
            category = "SEISMIC",
            timestampFormatted = "1h ago",
            severityTag = "INFO",
            sourceAgency = "Geological Survey BD"
        ),
        DisasterNewsItem(
            id = "news_3",
            title = "USGS Ring of Fire Advisory: M6.1 Subduction Zone Event in South Pacific",
            summary = "Tsunami evaluation confirms no trans-oceanic wave generation. Deep hypocenter at 142km depth prevented sea-floor displacement.",
            category = "TSUNAMI",
            timestampFormatted = "3h ago",
            severityTag = "WATCH",
            sourceAgency = "USGS Geological Survey"
        ),
        DisasterNewsItem(
            id = "news_4",
            title = "Team Omega IoT Sensor Calibration Completed for SW-420 Piezo Switch",
            summary = "High-sensitivity threshold calibration on GPIO 18 achieves 12ms edge-interrupt trigger response with BC547 buzzer isolation.",
            category = "SEISMIC",
            timestampFormatted = "4h ago",
            severityTag = "INFO",
            sourceAgency = "Team Omega Labs"
        )
    )
}
