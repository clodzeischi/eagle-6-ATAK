package com.atakmap.android.eagle6.model

import com.atakmap.coremap.maps.coords.GeoPoint
import java.util.UUID

data class Mission(
    val id: String = UUID.randomUUID().toString(),
    val pilot: String,
    val platform: String,
    val missionType: String,
    val launchTimeMs: Long,
    val launchLocation: GeoPoint,
    val infilWaypoints: List<GeoPoint> = emptyList(),
    val activityLocation: GeoPoint,
    val exfilWaypoints: List<GeoPoint> = emptyList(),
    val recoveryLocation: GeoPoint,
    val altitudeFt: String,
    val expectedDurationMin: Int,
    val confirmedAt: Long = System.currentTimeMillis(),
    val launchedAt: Long? = null,
    val completedAt: Long? = null
)
