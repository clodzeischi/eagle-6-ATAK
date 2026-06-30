package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStatus
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.coremap.maps.coords.GeoPoint

class LandingView(
    private val pluginContext: Context,
    private val selfLocation: () -> GeoPoint,
    private val onPickLocation: (prompt: String, callback: (GeoPoint) -> Unit) -> Unit,
    private val onLanded: (Mission, GeoPoint) -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_landing, null)

    private val txtLandingMgrs: TextView = view.findViewById(R.id.txt_landing_mgrs)
    private val waypointsContainer: LinearLayout = view.findViewById(R.id.landing_waypoints_container)

    private var mission: Mission? = null
    private var landingLocation: GeoPoint = selfLocation()
    private val waypoints = mutableListOf<GeoPoint>()

    init {
        view.findViewById<Button>(R.id.btn_pick_landing).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.landing_prompt_recovery)) { pt ->
                landingLocation = pt
                txtLandingMgrs.text = MessageFormatter.toMgrs(pt)
            }
        }
        view.findViewById<Button>(R.id.btn_reset_landing).setOnClickListener {
            landingLocation = selfLocation()
            txtLandingMgrs.text = MessageFormatter.toMgrs(landingLocation)
        }
        view.findViewById<Button>(R.id.btn_add_rtw_waypoint).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.landing_prompt_waypoint)) { pt ->
                waypoints.add(pt)
                refreshWaypoints()
            }
        }
        view.findViewById<Button>(R.id.btn_land).setOnClickListener {
            val m = mission ?: return@setOnClickListener
            m.status = MissionStatus.LANDED
            m.waypoints.clear()
            m.waypoints.addAll(waypoints)
            onLanded(m, landingLocation)
        }
    }

    fun bind(m: Mission) {
        mission = m
        landingLocation = selfLocation()
        waypoints.clear()
        txtLandingMgrs.text = MessageFormatter.toMgrs(landingLocation)
        refreshWaypoints()
        view.findViewById<TextView>(R.id.landing_pilot_platform).text =
            pluginContext.getString(R.string.landing_pilot_platform, m.pilot, m.platform)
    }

    private fun refreshWaypoints() {
        waypointsContainer.removeAllViews()
        waypoints.forEachIndexed { idx, pt ->
            val row = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_waypoint_row, null)
            row.findViewById<TextView>(R.id.waypoint_mgrs).text = "${idx + 1}. ${MessageFormatter.toMgrs(pt)}"
            row.findViewById<Button>(R.id.btn_remove_waypoint).setOnClickListener {
                waypoints.removeAt(idx)
                refreshWaypoints()
            }
            waypointsContainer.addView(row)
        }
    }
}
