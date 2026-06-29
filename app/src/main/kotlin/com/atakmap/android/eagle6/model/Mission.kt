package com.atakmap.android.eagle6.model

import com.atakmap.coremap.maps.coords.GeoPoint
import java.util.UUID

data class Mission(
    val id: String = UUID.randomUUID().toString(),
    val pilot: String,
    val platform: String,
    var missionType: String,
    val launchLocation: GeoPoint,
    val waypoints: MutableList<GeoPoint> = mutableListOf(),
    var activityLocation: GeoPoint,
    val altitudeFt: String,
    val expectedDurationMin: Int = 60,
    var status: MissionStatus = MissionStatus.LAUNCHING,
    val startTimeMs: Long = System.currentTimeMillis()
)
