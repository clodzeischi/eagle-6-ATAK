package com.atakmap.android.eagle6.map

import android.graphics.Color
import com.atakmap.android.maps.Association
import com.atakmap.android.maps.DefaultMapGroup
import com.atakmap.android.maps.MapGroup
import com.atakmap.android.maps.MapView
import com.atakmap.android.maps.Marker
import com.atakmap.android.overlay.DefaultMapGroupOverlay
import com.atakmap.android.util.Circle
import com.atakmap.coremap.maps.coords.GeoPoint
import com.atakmap.coremap.maps.coords.GeoPointMetaData

class PlanningPreviewRenderer(private val mapView: MapView) {

    // Ingress: light blue
    private val IN_R = 0x4F; private val IN_G = 0xC3; private val IN_B = 0xF7
    // Egress: amber
    private val EG_R = 0xFF; private val EG_G = 0x98; private val EG_B = 0x00

    private val FILL_ALPHA = 55
    private val INGRESS_ROUTE = Color.argb(180, IN_R, IN_G, IN_B)
    private val EGRESS_ROUTE  = Color.argb(180, EG_R, EG_G, EG_B)

    private val rootGroup: MapGroup = DefaultMapGroup("Eagle6_Planning")
    private val overlay = DefaultMapGroupOverlay(mapView, rootGroup)
    private var draftGroup: MapGroup? = null

    init {
        mapView.mapOverlayManager.addOverlay(overlay)
    }

    fun update(
        launch: GeoPoint,
        infilWps: List<GeoPoint>,
        activity: GeoPoint,
        exfilWps: List<GeoPoint>,
        recovery: GeoPoint,
        launchRadiusM: Double,
        activityRadiusM: Double
    ) {
        clearGroup()

        val group = DefaultMapGroup("Eagle6-draft")
        rootGroup.addGroup(group)
        draftGroup = group

        // Circles: launch + activity in blue, recovery in amber
        group.addItem(circle("e6-plan-launch",    launch,   launchRadiusM,   IN_R, IN_G, IN_B))
        group.addItem(circle("e6-plan-activity",  activity, activityRadiusM, IN_R, IN_G, IN_B))
        group.addItem(circle("e6-plan-recovery",  recovery, launchRadiusM,   EG_R, EG_G, EG_B))

        // Ingress route: launch → infil waypoints → activity
        val ingress = buildList { add(launch); addAll(infilWps); add(activity) }
        ingress.forEachIndexed { i, _ ->
            if (i < ingress.size - 1) addSegment(group, "in", i, ingress[i], ingress[i + 1], INGRESS_ROUTE)
        }

        // Egress route: activity → exfil waypoints → recovery
        val egress = buildList { add(activity); addAll(exfilWps); add(recovery) }
        egress.forEachIndexed { i, _ ->
            if (i < egress.size - 1) addSegment(group, "eg", i, egress[i], egress[i + 1], EGRESS_ROUTE)
        }
    }

    fun clear() = clearGroup()

    fun dispose() {
        clearGroup()
        mapView.mapOverlayManager.removeOverlay(overlay)
    }

    private fun clearGroup() {
        draftGroup?.let { g -> mapView.post { rootGroup.removeGroup(g) } }
        draftGroup = null
    }

    private fun circle(uid: String, pt: GeoPoint, radiusM: Double, r: Int, g: Int, b: Int) =
        Circle(GeoPointMetaData.wrap(pt), radiusM, uid).apply {
            setStrokeColor(Color.rgb(r, g, b))
            setFillColor(Color.argb(FILL_ALPHA, r, g, b))
            setStrokeWeight(2.0)
            setMetaBoolean("addToObjList", false)
        }

    private fun addSegment(group: MapGroup, prefix: String, i: Int, from: GeoPoint, to: GeoPoint, routeColor: Int) {
        val mFrom = Marker(GeoPointMetaData.wrap(from), "e6-$prefix-f-$i").apply {
            setMetaBoolean("addToObjList", false); setMetaBoolean("nevercot", true)
        }
        val mTo = Marker(GeoPointMetaData.wrap(to), "e6-$prefix-t-$i").apply {
            setMetaBoolean("addToObjList", false); setMetaBoolean("nevercot", true)
        }
        group.addItem(mFrom)
        group.addItem(mTo)
        group.addItem(Association(mFrom, mTo, "e6-$prefix-a-$i").apply {
            setColor(routeColor)
            setStrokeWeight(2.0)
            setLink(Association.LINK_LINE)
            setStyle(Association.STYLE_DASHED)
            setMetaBoolean("addToObjList", false)
        })
    }
}
