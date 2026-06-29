package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.model.Eagle6Settings
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.coremap.maps.coords.GeoPoint

class NewMissionView(
    private val pluginContext: Context,
    private val settings: Eagle6Settings,
    private val selfLocation: () -> GeoPoint,
    private val onPickLocation: (prompt: String, callback: (GeoPoint) -> Unit) -> Unit,
    private val onLaunch: (Mission) -> Unit,
    private val onCancel: () -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_new_mission, null)

    private val spinPilot: Spinner = view.findViewById(R.id.spin_pilot)
    private val spinPlatform: Spinner = view.findViewById(R.id.spin_platform)
    private val spinType: Spinner = view.findViewById(R.id.spin_mission_type)
    private val spinAltitude: Spinner = view.findViewById(R.id.spin_altitude)
    private val txtLaunchMgrs: TextView = view.findViewById(R.id.txt_launch_mgrs)
    private val txtActivityMgrs: TextView = view.findViewById(R.id.txt_activity_mgrs)
    private val waypointsContainer: LinearLayout = view.findViewById(R.id.waypoints_container)
    private val editDuration: EditText = view.findViewById(R.id.edit_duration)

    private var launchLocation: GeoPoint = selfLocation()
    private var activityLocation: GeoPoint = selfLocation()
    private val waypoints = mutableListOf<GeoPoint>()

    init {
        view.findViewById<Button>(R.id.btn_pick_launch).setOnClickListener {
            onPickLocation("Tap map to set launch location") { pt ->
                launchLocation = pt
                txtLaunchMgrs.text = MessageFormatter.toMgrs(pt)
            }
        }
        view.findViewById<Button>(R.id.btn_reset_launch).setOnClickListener {
            launchLocation = selfLocation()
            txtLaunchMgrs.text = MessageFormatter.toMgrs(launchLocation)
        }
        view.findViewById<Button>(R.id.btn_pick_activity).setOnClickListener {
            onPickLocation("Tap map to set activity location") { pt ->
                activityLocation = pt
                txtActivityMgrs.text = MessageFormatter.toMgrs(pt)
            }
        }
        view.findViewById<Button>(R.id.btn_reset_activity).setOnClickListener {
            activityLocation = selfLocation()
            txtActivityMgrs.text = MessageFormatter.toMgrs(activityLocation)
        }
        view.findViewById<Button>(R.id.btn_add_waypoint).setOnClickListener {
            onPickLocation("Tap map to add waypoint") { pt ->
                waypoints.add(pt)
                refreshWaypointList()
            }
        }
        view.findViewById<Button>(R.id.btn_launch).setOnClickListener { attemptLaunch() }
        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener { onCancel() }
    }

    fun refresh() {
        bindSpinner(spinPilot, settings.pilots, settings.lastPilotIndex)
        bindSpinner(spinPlatform, settings.platforms, settings.lastPlatformIndex)
        bindSpinner(spinType, settings.missionTypes, settings.lastMissionTypeIndex)
        bindSpinner(spinAltitude, settings.altitudes, settings.lastAltitudeIndex)
        editDuration.setText(settings.lastDurationMin.toString())

        launchLocation = selfLocation()
        activityLocation = selfLocation()
        waypoints.clear()
        txtLaunchMgrs.text = MessageFormatter.toMgrs(launchLocation)
        txtActivityMgrs.text = MessageFormatter.toMgrs(activityLocation)
        refreshWaypointList()
    }

    private fun bindSpinner(spinner: Spinner, items: List<String>, selectedIndex: Int) {
        val adapter = ArrayAdapter(pluginContext, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }

    private fun refreshWaypointList() {
        waypointsContainer.removeAllViews()
        waypoints.forEachIndexed { idx, pt ->
            val row = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_waypoint_row, null)
            row.findViewById<TextView>(R.id.waypoint_mgrs).text = "${idx + 1}. ${MessageFormatter.toMgrs(pt)}"
            row.findViewById<Button>(R.id.btn_remove_waypoint).setOnClickListener {
                waypoints.removeAt(idx)
                refreshWaypointList()
            }
            waypointsContainer.addView(row)
        }
    }

    private fun attemptLaunch() {
        val pilot = spinPilot.selectedItem?.toString() ?: return
        val platform = spinPlatform.selectedItem?.toString() ?: return
        val missionType = spinType.selectedItem?.toString() ?: return
        val altitude = spinAltitude.selectedItem?.toString() ?: return
        val duration = editDuration.text.toString().toIntOrNull() ?: 60

        if (pilot.isBlank() || platform.isBlank() || missionType.isBlank()) {
            Toast.makeText(pluginContext, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Persist last-used selections
        settings.lastPilotIndex = spinPilot.selectedItemPosition
        settings.lastPlatformIndex = spinPlatform.selectedItemPosition
        settings.lastMissionTypeIndex = spinType.selectedItemPosition
        settings.lastAltitudeIndex = spinAltitude.selectedItemPosition
        settings.lastDurationMin = duration

        val mission = Mission(
            pilot = pilot,
            platform = platform,
            missionType = missionType,
            launchLocation = launchLocation,
            waypoints = waypoints.toMutableList(),
            activityLocation = activityLocation,
            altitudeFt = altitude,
            expectedDurationMin = duration
        )
        onLaunch(mission)
    }
}
