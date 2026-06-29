package com.atakmap.android.eagle6.map

import android.os.Bundle
import com.atakmap.android.maps.MapEvent
import com.atakmap.android.maps.MapEventDispatcher
import com.atakmap.android.maps.MapView
import com.atakmap.android.maps.PointMapItem
import com.atakmap.android.toolbar.Tool
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver
import com.atakmap.android.toolbar.widgets.TextContainer
import com.atakmap.coremap.maps.coords.GeoPoint

class LocationPickerTool(
    mapView: MapView,
    private val prompt: String,
    private val onPicked: (GeoPoint) -> Unit
) : Tool(mapView, TOOL_ID), MapEventDispatcher.MapEventDispatchListener {

    companion object {
        const val TOOL_ID = "com.atakmap.android.eagle6.LocationPickerTool"

        fun register(mapView: MapView) {
            // Tool is registered per-use; this just ensures the ID is known.
        }
    }

    init {
        ToolManagerBroadcastReceiver.getInstance().registerTool(TOOL_ID, this)
    }

    override fun onToolBegin(extras: Bundle?): Boolean {
        TextContainer.getInstance().displayPrompt(prompt)
        _mapView.mapEventDispatcher.pushListeners()
        _mapView.mapEventDispatcher.addMapEventListener(MapEvent.MAP_CLICK, this)
        _mapView.mapEventDispatcher.addMapEventListener(MapEvent.ITEM_CLICK, this)
        return true
    }

    override fun onToolEnd() {
        _mapView.mapEventDispatcher.removeMapEventListener(MapEvent.MAP_CLICK, this)
        _mapView.mapEventDispatcher.removeMapEventListener(MapEvent.ITEM_CLICK, this)
        _mapView.mapEventDispatcher.popListeners()
        TextContainer.getInstance().closePrompt()
        super.onToolEnd()
    }

    override fun onMapEvent(event: MapEvent) {
        val point: GeoPoint? = when (event.type) {
            MapEvent.MAP_CLICK  -> event.pointF?.let { _mapView.inverseWithElevation(it.x, it.y)?.get() }
            MapEvent.ITEM_CLICK -> (event.item as? PointMapItem)?.geoPointMetaData?.get()
            else -> null
        }
        if (point != null) {
            requestEndTool()
            onPicked(point)
        }
    }

    override fun dispose() {
        ToolManagerBroadcastReceiver.getInstance().unregisterTool(TOOL_ID)
    }
}
