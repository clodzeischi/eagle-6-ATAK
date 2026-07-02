package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.chat.MessageFormatter
import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.coremap.maps.coords.GeoPoint

class MissionEditView(
    private val pluginContext: Context,
    private val selfLocation: () -> GeoPoint,
    private val onPickLocation: (prompt: String, callback: (GeoPoint) -> Unit) -> Unit,
    private val onPickLaunchTime: (callback: (Long) -> Unit) -> Unit,
    private val onSave: (original: Mission, updated: Mission) -> Unit,
    private val onCancelMission: (Mission) -> Unit,
    private val onBack: () -> Unit,
    private val onPreviewUpdate: (GeoPoint, List<GeoPoint>, GeoPoint, List<GeoPoint>, GeoPoint) -> Unit = { _, _, _, _, _ -> }
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_edit, null)

    private val spinPilot: Spinner = view.findViewById(R.id.spin_pilot)
    private val spinPlatform: Spinner = view.findViewById(R.id.spin_platform)
    private val spinType: Spinner = view.findViewById(R.id.spin_mission_type)
    private val spinAltitude: Spinner = view.findViewById(R.id.spin_altitude)
    private val txtLaunchTime: TextView = view.findViewById(R.id.txt_launch_time)
    private val txtLaunchMgrs: TextView = view.findViewById(R.id.txt_launch_mgrs)
    private val txtActivityMgrs: TextView = view.findViewById(R.id.txt_activity_mgrs)
    private val txtRecoveryMgrs: TextView = view.findViewById(R.id.txt_recovery_mgrs)
    private val infilContainer: LinearLayout = view.findViewById(R.id.infil_waypoints_container)
    private val exfilContainer: LinearLayout = view.findViewById(R.id.exfil_waypoints_container)
    private val editDuration: EditText = view.findViewById(R.id.edit_duration)

    private var original: Mission? = null
    private var launchTimeMs: Long = 0L
    private var launchLocation: GeoPoint = selfLocation()
    private var activityLocation: GeoPoint = selfLocation()
    private var recoveryLocation: GeoPoint = selfLocation()
    private val infilWaypoints = mutableListOf<GeoPoint>()
    private val exfilWaypoints = mutableListOf<GeoPoint>()

    private fun notifyPreview() {
        onPreviewUpdate(launchLocation, infilWaypoints.toList(), activityLocation, exfilWaypoints.toList(), recoveryLocation)
    }

    init {
        view.findViewById<Button>(R.id.btn_back).setOnClickListener { onBack() }
        view.findViewById<Button>(R.id.btn_pick_launch_time).setOnClickListener {
            onPickLaunchTime { ms ->
                launchTimeMs = ms
                txtLaunchTime.text = MessageFormatter.displayTime(ms)
            }
        }
        view.findViewById<Button>(R.id.btn_pick_launch).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.new_mission_prompt_launch)) { pt ->
                launchLocation = pt
                txtLaunchMgrs.text = MessageFormatter.toMgrs(pt)
                notifyPreview()
            }
        }
        view.findViewById<Button>(R.id.btn_reset_launch).setOnClickListener {
            launchLocation = selfLocation()
            txtLaunchMgrs.text = MessageFormatter.toMgrs(launchLocation)
            notifyPreview()
        }
        view.findViewById<Button>(R.id.btn_add_infil_waypoint).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.new_mission_prompt_infil_waypoint)) { pt ->
                infilWaypoints.add(pt)
                refreshWaypoints(infilContainer, infilWaypoints)
            }
        }
        view.findViewById<Button>(R.id.btn_pick_activity).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.new_mission_prompt_activity)) { pt ->
                activityLocation = pt
                txtActivityMgrs.text = MessageFormatter.toMgrs(pt)
                notifyPreview()
            }
        }
        view.findViewById<Button>(R.id.btn_reset_activity).setOnClickListener {
            activityLocation = selfLocation()
            txtActivityMgrs.text = MessageFormatter.toMgrs(activityLocation)
            notifyPreview()
        }
        view.findViewById<Button>(R.id.btn_add_exfil_waypoint).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.new_mission_prompt_exfil_waypoint)) { pt ->
                exfilWaypoints.add(pt)
                refreshWaypoints(exfilContainer, exfilWaypoints)
            }
        }
        view.findViewById<Button>(R.id.btn_pick_recovery).setOnClickListener {
            onPickLocation(pluginContext.getString(R.string.new_mission_prompt_recovery)) { pt ->
                recoveryLocation = pt
                txtRecoveryMgrs.text = MessageFormatter.toMgrs(pt)
                notifyPreview()
            }
        }
        view.findViewById<Button>(R.id.btn_reset_recovery).setOnClickListener {
            recoveryLocation = selfLocation()
            txtRecoveryMgrs.text = MessageFormatter.toMgrs(recoveryLocation)
            notifyPreview()
        }
        view.findViewById<Button>(R.id.btn_save).setOnClickListener { attemptSave() }
        view.findViewById<Button>(R.id.btn_cancel_mission).setOnClickListener {
            original?.let { onCancelMission(it) }
        }
    }

    fun bind(mission: Mission) {
        original = mission

        bindSpinner(spinPilot, Eagle6Prefs.pilots, Eagle6Prefs.pilots.indexOf(mission.pilot).coerceAtLeast(0))
        bindSpinner(spinPlatform, Eagle6Prefs.platforms, Eagle6Prefs.platforms.indexOf(mission.platform).coerceAtLeast(0))
        bindSpinner(spinType, Eagle6Prefs.missionTypes, Eagle6Prefs.missionTypes.indexOf(mission.missionType).coerceAtLeast(0))
        bindSpinner(spinAltitude, Eagle6Prefs.altitudes, Eagle6Prefs.altitudes.indexOf(mission.altitudeFt).coerceAtLeast(0))

        launchTimeMs = mission.launchTimeMs
        txtLaunchTime.text = MessageFormatter.displayTime(mission.launchTimeMs)

        launchLocation = mission.launchLocation
        txtLaunchMgrs.text = MessageFormatter.toMgrs(mission.launchLocation)

        activityLocation = mission.activityLocation
        txtActivityMgrs.text = MessageFormatter.toMgrs(mission.activityLocation)

        recoveryLocation = mission.recoveryLocation
        txtRecoveryMgrs.text = MessageFormatter.toMgrs(mission.recoveryLocation)

        infilWaypoints.clear()
        infilWaypoints.addAll(mission.infilWaypoints)
        refreshWaypoints(infilContainer, infilWaypoints)

        exfilWaypoints.clear()
        exfilWaypoints.addAll(mission.exfilWaypoints)
        refreshWaypoints(exfilContainer, exfilWaypoints)

        editDuration.setText(mission.expectedDurationMin.toString())
        notifyPreview()
    }

    private fun bindSpinner(spinner: Spinner, items: List<String>, selectedIndex: Int) {
        val adapter = ArrayAdapter(pluginContext, R.layout.e6_spinner_item, items)
        adapter.setDropDownViewResource(R.layout.e6_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }

    private fun refreshWaypoints(container: LinearLayout, waypoints: MutableList<GeoPoint>) {
        container.removeAllViews()
        waypoints.forEachIndexed { idx, pt ->
            val row = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_waypoint_row, null)
            row.findViewById<TextView>(R.id.waypoint_mgrs).text = "${idx + 1}. ${MessageFormatter.toMgrs(pt)}"
            row.findViewById<Button>(R.id.btn_remove_waypoint).setOnClickListener {
                waypoints.removeAt(idx)
                refreshWaypoints(container, waypoints)
            }
            container.addView(row)
        }
        notifyPreview()
    }

    private fun attemptSave() {
        val m = original ?: return
        val pilot = spinPilot.selectedItem?.toString()
        val platform = spinPlatform.selectedItem?.toString()
        val missionType = spinType.selectedItem?.toString()
        val altitude = spinAltitude.selectedItem?.toString()
        val duration = editDuration.text.toString().toIntOrNull()

        if (pilot.isNullOrBlank() || platform.isNullOrBlank() || missionType.isNullOrBlank()
            || altitude.isNullOrBlank() || duration == null || duration <= 0) {
            Toast.makeText(pluginContext, R.string.new_mission_error_fill_fields, Toast.LENGTH_SHORT).show()
            return
        }

        val updated = m.copy(
            pilot = pilot,
            platform = platform,
            missionType = missionType,
            launchTimeMs = launchTimeMs,
            launchLocation = launchLocation,
            infilWaypoints = infilWaypoints.toList(),
            activityLocation = activityLocation,
            exfilWaypoints = exfilWaypoints.toList(),
            recoveryLocation = recoveryLocation,
            altitudeFt = altitude,
            expectedDurationMin = duration
        )
        onSave(m, updated)
    }
}
