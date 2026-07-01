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

    private val R = 0x4F; private val G = 0xC3; private val B = 0xF7
    private val FILL_ALPHA = 55
    private val ROUTE_COLOR = Color.argb(180, R, G, B)

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

        group.addItem(circle("e6-plan-launch", launch, launchRadiusM))
        group.addItem(circle("e6-plan-activity", activity, activityRadiusM))

        val pts = buildList {
            add(launch)
            addAll(infilWps)
            add(activity)
            addAll(exfilWps)
            add(recovery)
        }
        pts.forEachIndexed { i, _ ->
            if (i < pts.size - 1) addSegment(group, i, pts[i], pts[i + 1])
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

    private fun circle(uid: String, pt: GeoPoint, radiusM: Double) =
        Circle(GeoPointMetaData.wrap(pt), radiusM, uid).apply {
            setStrokeColor(Color.rgb(R, G, B))
            setFillColor(Color.argb(FILL_ALPHA, R, G, B))
            setStrokeWeight(2.0)
            setMetaBoolean("addToObjList", false)
        }

    private fun addSegment(group: MapGroup, i: Int, from: GeoPoint, to: GeoPoint) {
        val mFrom = Marker(GeoPointMetaData.wrap(from), "e6-pf-$i").apply {
            setMetaBoolean("addToObjList", false); setMetaBoolean("nevercot", true)
        }
        val mTo = Marker(GeoPointMetaData.wrap(to), "e6-pt-$i").apply {
            setMetaBoolean("addToObjList", false); setMetaBoolean("nevercot", true)
        }
        group.addItem(mFrom)
        group.addItem(mTo)
        group.addItem(Association(mFrom, mTo, "e6-pa-$i").apply {
            setColor(ROUTE_COLOR)
            setStrokeWeight(2.0)
            setLink(Association.LINK_LINE)
            setStyle(Association.STYLE_DASHED)
            setMetaBoolean("addToObjList", false)
        })
    }
}
