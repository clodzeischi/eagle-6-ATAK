package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.model.Eagle6Settings
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStatus
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.coremap.maps.coords.GeoPoint

class MissionDetailView(
    private val pluginContext: Context,
    private val settings: Eagle6Settings,
    private val onPickLocation: (prompt: String, callback: (GeoPoint) -> Unit) -> Unit,
    private val onMissionUpdated: (Mission, String) -> Unit,  // mission + message for COT/chat
    private val onRth: (Mission) -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_detail, null)

    private val txtPilot: TextView = view.findViewById(R.id.detail_pilot)
    private val txtPlatform: TextView = view.findViewById(R.id.detail_platform)
    private val txtType: TextView = view.findViewById(R.id.detail_type)
    private val txtActivityMgrs: TextView = view.findViewById(R.id.detail_activity_mgrs)
    private val txtStatus: TextView = view.findViewById(R.id.detail_status)
    private val btnPrimaryAction: Button = view.findViewById(R.id.btn_primary_action)
    private val spinRetaskType: Spinner = view.findViewById(R.id.spin_retask_type)
    private val btnRetaskType: Button = view.findViewById(R.id.btn_retask_type)
    private val btnRetaskLocation: Button = view.findViewById(R.id.btn_retask_location)
    private val btnRth: Button = view.findViewById(R.id.btn_rth)

    private var mission: Mission? = null

    fun bind(m: Mission) {
        mission = m
        refresh()
    }

    private fun refresh() {
        val m = mission ?: return
        txtPilot.text = m.pilot
        txtPlatform.text = m.platform
        txtType.text = m.missionType
        txtActivityMgrs.text = MessageFormatter.toMgrs(m.activityLocation)
        txtStatus.text = m.status.displayName()

        // Primary action depends on status
        when (m.status) {
            MissionStatus.LAUNCHING -> {
                btnPrimaryAction.visibility = View.VISIBLE
                btnPrimaryAction.text = "MARK ON TASK"
                btnPrimaryAction.setOnClickListener { markOnTask() }
                btnRetaskType.visibility = View.GONE
                btnRetaskLocation.visibility = View.GONE
                spinRetaskType.visibility = View.GONE
                btnRth.visibility = View.GONE
            }
            MissionStatus.ON_TASK -> {
                btnPrimaryAction.visibility = View.GONE
                btnRetaskType.visibility = View.VISIBLE
                spinRetaskType.visibility = View.VISIBLE
                btnRetaskLocation.visibility = View.VISIBLE
                btnRth.visibility = View.VISIBLE
                bindRetaskTypeSpinner()
                btnRetaskType.setOnClickListener { retaskType() }
                btnRetaskLocation.setOnClickListener { retaskLocation() }
                btnRth.setOnClickListener { startRth() }
            }
            else -> {
                btnPrimaryAction.visibility = View.GONE
                btnRetaskType.visibility = View.GONE
                spinRetaskType.visibility = View.GONE
                btnRetaskLocation.visibility = View.GONE
                btnRth.visibility = View.GONE
            }
        }
    }

    private fun bindRetaskTypeSpinner() {
        val types = settings.missionTypes
        val adapter = ArrayAdapter(pluginContext, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinRetaskType.adapter = adapter
        val m = mission ?: return
        val idx = types.indexOf(m.missionType).coerceAtLeast(0)
        spinRetaskType.setSelection(idx)
    }

    private fun markOnTask() {
        val m = mission ?: return
        m.status = MissionStatus.ON_TASK
        val msg = MessageFormatter.onTaskMessage(m.pilot)
        onMissionUpdated(m, msg)
        refresh()
    }

    private fun retaskType() {
        val m = mission ?: return
        val newType = spinRetaskType.selectedItem?.toString() ?: return
        if (newType == m.missionType) return
        m.missionType = newType
        val msg = MessageFormatter.retaskTypeMessage(m.pilot, newType)
        onMissionUpdated(m, msg)
        refresh()
    }

    private fun retaskLocation() {
        val m = mission ?: return
        onPickLocation("Tap map to set new activity location") { pt ->
            m.activityLocation = pt
            val msg = MessageFormatter.retaskLocationMessage(m.pilot, MessageFormatter.toMgrs(pt), m.altitudeFt)
            onMissionUpdated(m, msg)
            txtActivityMgrs.text = MessageFormatter.toMgrs(pt)
        }
    }

    private fun startRth() {
        val m = mission ?: return
        m.status = MissionStatus.RTH
        onRth(m)
    }
}
