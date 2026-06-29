package com.atakmap.android.eagle6.cot

import com.atakmap.android.cot.detail.CotDetailHandler
import com.atakmap.android.eagle6.map.ReceivedMissionRenderer
import com.atakmap.android.maps.MapItem
import com.atakmap.android.maps.MapView
import com.atakmap.comms.CommsMapComponent
import com.atakmap.coremap.cot.event.CotDetail
import com.atakmap.coremap.cot.event.CotEvent
import com.atakmap.coremap.maps.coords.GeoPoint

class Eagle6DetailHandler(private val mapView: MapView) : CotDetailHandler("__eagle-detail") {

    var renderer: ReceivedMissionRenderer? = null

    override fun toItemMetadata(item: MapItem, event: CotEvent, detail: CotDetail): CommsMapComponent.ImportResult {
        val r = renderer ?: return CommsMapComponent.ImportResult.FAILURE
        val missionId = detail.getAttribute("missionId") ?: return CommsMapComponent.ImportResult.FAILURE
        val pilot = detail.getAttribute("pilot") ?: return CommsMapComponent.ImportResult.FAILURE
        val platform = detail.getAttribute("platform") ?: ""
        val missionType = detail.getAttribute("missionType") ?: ""
        val status = detail.getAttribute("status") ?: ""
        val altitudeFt = detail.getAttribute("altitudeFt") ?: ""
        val launchLat = detail.getAttribute("launchLat")?.toDoubleOrNull() ?: return CommsMapComponent.ImportResult.FAILURE
        val launchLon = detail.getAttribute("launchLon")?.toDoubleOrNull() ?: return CommsMapComponent.ImportResult.FAILURE
        val launchHae = detail.getAttribute("launchHae")?.toDoubleOrNull() ?: 0.0
        val actLat = detail.getAttribute("activityLat")?.toDoubleOrNull() ?: return CommsMapComponent.ImportResult.FAILURE
        val actLon = detail.getAttribute("activityLon")?.toDoubleOrNull() ?: return CommsMapComponent.ImportResult.FAILURE
        val actHae = detail.getAttribute("activityHae")?.toDoubleOrNull() ?: 0.0
        val launchRadius = detail.getAttribute("launchZoneRadius")?.toDoubleOrNull() ?: 50.0
        val actRadius = detail.getAttribute("activityZoneRadius")?.toDoubleOrNull() ?: 300.0
        val waypointsRaw = detail.getAttribute("waypoints") ?: ""

        val waypoints = if (waypointsRaw.isNotBlank()) {
            waypointsRaw.split(";").mapNotNull { seg ->
                val parts = seg.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lon = parts[1].toDoubleOrNull()
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
                } else null
            }
        } else emptyList()

        val staleMs = event.stale?.milliseconds ?: (System.currentTimeMillis() + 3_600_000L)

        mapView.post {
            r.render(
                missionId = missionId,
                pilot = pilot,
                platform = platform,
                missionType = missionType,
                status = status,
                altitudeFt = altitudeFt,
                launchPoint = GeoPoint(launchLat, launchLon, launchHae),
                activityPoint = GeoPoint(actLat, actLon, actHae),
                waypoints = waypoints,
                launchZoneRadiusM = launchRadius,
                activityZoneRadiusM = actRadius,
                staleTimeMs = staleMs
            )
        }
        return CommsMapComponent.ImportResult.SUCCESS
    }

    override fun toCotDetail(item: MapItem, event: CotEvent, detail: CotDetail): Boolean = false
}
