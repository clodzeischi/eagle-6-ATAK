package com.atakmap.android.eagle6.map

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import com.atakmap.android.maps.Association
import com.atakmap.android.maps.DefaultMapGroup
import com.atakmap.android.maps.MapGroup
import com.atakmap.android.maps.MapView
import com.atakmap.android.maps.Marker
import com.atakmap.android.overlay.DefaultMapGroupOverlay
import com.atakmap.android.util.Circle
import com.atakmap.coremap.maps.coords.GeoPoint
import com.atakmap.coremap.maps.coords.GeoPointMetaData
import java.util.concurrent.ConcurrentHashMap

class ReceivedMissionRenderer(private val mapView: MapView) {

    private val BLUE_R = 0x4F; private val BLUE_G = 0xC3; private val BLUE_B = 0xF7
    private val FILL_ALPHA = 55  // ~22% opacity
    private val ROUTE_COLOR = Color.argb(180, BLUE_R, BLUE_G, BLUE_B)

    private data class Rendered(
        val group: MapGroup,
        val launchCircle: Circle,
        val animator: ValueAnimator,
        val handler: Handler,
        val cleanup: Runnable
    )

    private val active = ConcurrentHashMap<String, Rendered>()
    private val rootGroup: MapGroup = DefaultMapGroup("Eagle6_Remote")
    private val overlay = DefaultMapGroupOverlay(mapView, rootGroup)

    init {
        mapView.mapOverlayManager.addOverlay(overlay)
    }

    fun render(
        missionId: String,
        pilot: String,
        platform: String,
        missionType: String,
        status: String,
        altitudeFt: String,
        launchPoint: GeoPoint,
        activityPoint: GeoPoint,
        waypoints: List<GeoPoint>,
        launchZoneRadiusM: Double,
        activityZoneRadiusM: Double,
        staleTimeMs: Long
    ) {
        remove(missionId)

        val group = DefaultMapGroup("Eagle6-$missionId")
        rootGroup.addGroup(group)

        // Launch / recovery zone — pulsating
        val launchCircle = Circle(
            GeoPointMetaData.wrap(launchPoint),
            launchZoneRadiusM,
            "e6-launch-$missionId"
        ).apply {
            setStrokeColor(Color.rgb(BLUE_R, BLUE_G, BLUE_B))
            setFillColor(Color.argb(FILL_ALPHA, BLUE_R, BLUE_G, BLUE_B))
            setStrokeWeight(2.0)
            setMetaBoolean("addToObjList", false)
            setMetaString("shapeName", "$pilot launch")
        }
        group.addItem(launchCircle)

        // Activity area — static
        val activityCircle = Circle(
            GeoPointMetaData.wrap(activityPoint),
            activityZoneRadiusM,
            "e6-activity-$missionId"
        ).apply {
            setStrokeColor(Color.rgb(BLUE_R, BLUE_G, BLUE_B))
            setFillColor(Color.argb(FILL_ALPHA, BLUE_R, BLUE_G, BLUE_B))
            setStrokeWeight(2.0)
            setMetaBoolean("addToObjList", false)
            setMetaString("shapeName", "$pilot activity")
        }
        group.addItem(activityCircle)

        // Route: launch → wp1 → wp2 → activity → wp2 → wp1 → launch
        val routePoints = buildList {
            add(launchPoint)
            addAll(waypoints)
            add(activityPoint)
            addAll(waypoints.reversed())
            add(launchPoint)
        }
        routePoints.forEachIndexed { i, _ ->
            if (i < routePoints.size - 1) addRouteSegment(group, missionId, i, routePoints[i], routePoints[i + 1])
        }

        // Sine-wave opacity animation on launch circle
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val alpha = (FILL_ALPHA * (0.3f + 0.7f * t)).toInt()
                mapView.post { launchCircle.setFillColor(Color.argb(alpha, BLUE_R, BLUE_G, BLUE_B)) }
            }
            start()
        }

        val handler = Handler(Looper.getMainLooper())
        val cleanup = Runnable { remove(missionId) }
        handler.postDelayed(cleanup, (staleTimeMs - System.currentTimeMillis()).coerceAtLeast(0))

        active[missionId] = Rendered(group, launchCircle, animator, handler, cleanup)
    }

    private fun addRouteSegment(group: MapGroup, missionId: String, i: Int, from: GeoPoint, to: GeoPoint) {
        val mFrom = Marker(GeoPointMetaData.wrap(from), "e6-rf-$missionId-$i").apply {
            setMetaBoolean("addToObjList", false)
            setMetaBoolean("nevercot", true)
        }
        val mTo = Marker(GeoPointMetaData.wrap(to), "e6-rt-$missionId-$i").apply {
            setMetaBoolean("addToObjList", false)
            setMetaBoolean("nevercot", true)
        }
        group.addItem(mFrom)
        group.addItem(mTo)
        val assoc = Association(mFrom, mTo, "e6-ra-$missionId-$i").apply {
            setColor(ROUTE_COLOR)
            setStrokeWeight(2.0)
            setLink(Association.LINK_LINE)
            setStyle(Association.STYLE_DASHED)
            setMetaBoolean("addToObjList", false)
        }
        group.addItem(assoc)
    }

    fun remove(missionId: String) {
        val r = active.remove(missionId) ?: return
        r.animator.cancel()
        r.handler.removeCallbacks(r.cleanup)
        mapView.post { rootGroup.removeGroup(r.group) }
    }

    fun dispose() {
        active.keys.toList().forEach { remove(it) }
        mapView.mapOverlayManager.removeOverlay(overlay)
    }
}
