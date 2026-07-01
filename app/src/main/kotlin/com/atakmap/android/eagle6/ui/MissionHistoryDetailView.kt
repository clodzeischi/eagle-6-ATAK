package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.plugintemplate.plugin.R

class MissionHistoryDetailView(
    private val pluginContext: Context,
    private val onBack: () -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_history_detail, null)

    init {
        view.findViewById<Button>(R.id.btn_back).setOnClickListener { onBack() }
    }

    fun bind(mission: Mission) {
        view.findViewById<TextView>(R.id.history_completed_at).text =
            mission.completedAt?.let { MessageFormatter.timeStr(it) } ?: ""
        view.findViewById<TextView>(R.id.history_pilot).text = mission.pilot
        view.findViewById<TextView>(R.id.history_platform).text = mission.platform
        view.findViewById<TextView>(R.id.history_type).text = mission.missionType
        view.findViewById<TextView>(R.id.history_launch_time).text =
            MessageFormatter.displayTime(mission.launchTimeMs)
        view.findViewById<TextView>(R.id.history_launch).text =
            MessageFormatter.toMgrs(mission.launchLocation)
        view.findViewById<TextView>(R.id.history_activity).text =
            MessageFormatter.toMgrs(mission.activityLocation)
        view.findViewById<TextView>(R.id.history_recovery).text =
            MessageFormatter.toMgrs(mission.recoveryLocation)
        view.findViewById<TextView>(R.id.history_altitude).text = "${mission.altitudeFt}'"
        view.findViewById<TextView>(R.id.history_duration).text = "${mission.expectedDurationMin} min"
    }
}
