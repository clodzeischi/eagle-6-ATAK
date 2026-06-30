package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStatus
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.coremap.maps.coords.GeoPoint

class MissionDetailView(
    private val pluginContext: Context,
    private val onPickLocation: (prompt: String, callback: (GeoPoint) -> Unit) -> Unit,
    private val onMissionUpdated: (Mission, String) -> Unit,
    private val onRth: (Mission) -> Unit,
    private val onCancel: (Mission) -> Unit,
    private val onBack: () -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_detail, null)

    private val txtPilot: TextView = view.findViewById(R.id.detail_pilot)
    private val txtPlatform: TextView = view.findViewById(R.id.detail_platform)
    private val txtType: TextView = view.findViewById(R.id.detail_type)
    private val txtActivityMgrs: TextView = view.findViewById(R.id.detail_activity_mgrs)
    private val txtStatus: TextView = view.findViewById(R.id.detail_status)
    private val btnPrimaryAction: Button = view.findViewById(R.id.btn_primary_action)
    private val rowRetaskType: LinearLayout = view.findViewById(R.id.row_retask_type)
    private val spinRetaskType: Spinner = view.findViewById(R.id.spin_retask_type)
    private val btnRetaskType: Button = view.findViewById(R.id.btn_retask_type)
    private val btnRetaskLocation: Button = view.findViewById(R.id.btn_retask_location)
    private val btnRth: Button = view.findViewById(R.id.btn_rth)
    private val btnCancel: Button = view.findViewById(R.id.btn_cancel)

    private var mission: Mission? = null

    init {
        view.findViewById<Button>(R.id.btn_back).setOnClickListener { onBack() }
    }

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

        when (m.status) {
            MissionStatus.LAUNCHING -> {
                btnPrimaryAction.visibility = View.VISIBLE
                btnPrimaryAction.text = pluginContext.getString(R.string.detail_action_mark_on_task)
                btnPrimaryAction.setOnClickListener { markOnTask() }
                rowRetaskType.visibility = View.GONE
                btnRetaskLocation.visibility = View.GONE
                btnRth.visibility = View.GONE
                btnCancel.visibility = View.VISIBLE
                btnCancel.text = pluginContext.getString(R.string.detail_action_cancel_mission)
                btnCancel.setOnClickListener { onCancel(m) }
            }
            MissionStatus.ON_TASK -> {
                btnPrimaryAction.visibility = View.GONE
                rowRetaskType.visibility = View.VISIBLE
                btnRetaskLocation.visibility = View.VISIBLE
                btnRth.visibility = View.VISIBLE
                btnCancel.visibility = View.GONE
                bindRetaskTypeSpinner()
                btnRetaskType.setOnClickListener { retaskType() }
                btnRetaskLocation.setOnClickListener { retaskLocation() }
                btnRth.setOnClickListener { startRth() }
            }
            MissionStatus.RTH -> {
                btnPrimaryAction.visibility = View.GONE
                rowRetaskType.visibility = View.GONE
                btnRetaskLocation.visibility = View.GONE
                btnRth.visibility = View.GONE
                btnCancel.visibility = View.VISIBLE
                btnCancel.text = pluginContext.getString(R.string.common_cancel_rth)
                btnCancel.setOnClickListener { onCancel(m) }
            }
            else -> {
                btnPrimaryAction.visibility = View.GONE
                rowRetaskType.visibility = View.GONE
                btnRetaskLocation.visibility = View.GONE
                btnRth.visibility = View.GONE
                btnCancel.visibility = View.GONE
            }
        }
    }

    private fun bindRetaskTypeSpinner() {
        val types = Eagle6Prefs.missionTypes
        val adapter = ArrayAdapter(pluginContext, R.layout.e6_spinner_item, types)
        adapter.setDropDownViewResource(R.layout.e6_spinner_dropdown_item)
        spinRetaskType.adapter = adapter
        val m = mission ?: return
        spinRetaskType.setSelection(types.indexOf(m.missionType).coerceAtLeast(0))
    }

    private fun markOnTask() {
        val m = mission ?: return
        m.status = MissionStatus.ON_TASK
        onMissionUpdated(m, MessageFormatter.onTaskMessage(m.pilot))
        refresh()
    }

    private fun retaskType() {
        val m = mission ?: return
        val newType = spinRetaskType.selectedItem?.toString() ?: return
        if (newType == m.missionType) return
        m.missionType = newType
        onMissionUpdated(m, MessageFormatter.retaskTypeMessage(m.pilot, newType))
        refresh()
    }

    private fun retaskLocation() {
        val m = mission ?: return
        onPickLocation(pluginContext.getString(R.string.detail_prompt_activity)) { pt ->
            m.activityLocation = pt
            onMissionUpdated(m, MessageFormatter.retaskLocationMessage(m.pilot, MessageFormatter.toMgrs(pt), m.altitudeFt))
            txtActivityMgrs.text = MessageFormatter.toMgrs(pt)
        }
    }

    private fun startRth() {
        val m = mission ?: return
        m.status = MissionStatus.RTH
        onRth(m)
    }
}
