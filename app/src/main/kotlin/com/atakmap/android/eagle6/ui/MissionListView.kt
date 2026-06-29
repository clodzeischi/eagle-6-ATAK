package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStatus
import com.atakmap.android.eagle6.model.MissionStore
import com.atakmap.android.plugintemplate.plugin.R

class MissionListView(
    private val pluginContext: Context,
    private val onNewMission: () -> Unit,
    private val onOpenMission: (Mission) -> Unit,
    private val onSettings: () -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_list, null)

    private val missionsContainer: LinearLayout = view.findViewById(R.id.missions_container)
    private val emptyLabel: TextView = view.findViewById(R.id.empty_label)

    init {
        view.findViewById<Button>(R.id.btn_new_mission).setOnClickListener { onNewMission() }
        view.findViewById<Button>(R.id.btn_settings).setOnClickListener { onSettings() }
    }

    fun refresh() {
        val missions = MissionStore.missions
        emptyLabel.visibility = if (missions.isEmpty()) View.VISIBLE else View.GONE
        missionsContainer.removeAllViews()
        missions.forEach { mission -> missionsContainer.addView(buildCard(mission)) }
    }

    private fun buildCard(mission: Mission): View {
        val card = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_card, null)
        card.findViewById<TextView>(R.id.card_pilot).text = mission.pilot
        card.findViewById<TextView>(R.id.card_platform).text = mission.platform
        card.findViewById<TextView>(R.id.card_type).text = mission.missionType
        card.findViewById<TextView>(R.id.card_status).apply {
            text = mission.status.displayName()
            setTextColor(statusColor(mission.status))
        }
        card.setOnClickListener { onOpenMission(mission) }
        return card
    }

    private fun statusColor(status: MissionStatus): Int = when (status) {
        MissionStatus.LAUNCHING -> 0xFFFFD54F.toInt()  // amber
        MissionStatus.ON_TASK   -> 0xFF81C784.toInt()  // green
        MissionStatus.RTH       -> 0xFFFF8A65.toInt()  // orange
        MissionStatus.LANDED    -> 0xFF90A4AE.toInt()  // gray
    }
}
