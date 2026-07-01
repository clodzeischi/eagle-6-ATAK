package com.atakmap.android.eagle6.ui

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStore
import com.atakmap.android.plugintemplate.plugin.R

class MissionListView(
    private val pluginContext: Context,
    private val onNewMission: () -> Unit,
    private val onOpenMission: (Mission) -> Unit,
    private val onOpenHistory: (Mission) -> Unit,
    private val onSettings: () -> Unit,
    private val onClearComplete: () -> Unit
) {
    val view: View = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_list, null)

    private val missionsContainer: LinearLayout = view.findViewById(R.id.missions_container)
    private val emptyLabel: TextView = view.findViewById(R.id.empty_label)
    private val historySection: LinearLayout = view.findViewById(R.id.history_section)
    private val historyContainer: LinearLayout = view.findViewById(R.id.history_container)

    init {
        view.findViewById<Button>(R.id.btn_new_mission).setOnClickListener { onNewMission() }
        view.findViewById<Button>(R.id.btn_settings).setOnClickListener { onSettings() }
        view.findViewById<Button>(R.id.btn_clear_complete).setOnClickListener { onClearComplete() }
    }

    fun refresh() {
        val active = MissionStore.activeMissions
        val completed = MissionStore.completedMissions

        emptyLabel.visibility = if (active.isEmpty()) View.VISIBLE else View.GONE
        missionsContainer.removeAllViews()
        active.forEach { missionsContainer.addView(buildCard(it, isHistory = false)) }

        historySection.visibility = if (completed.isEmpty()) View.GONE else View.VISIBLE
        historyContainer.removeAllViews()
        completed.forEach { historyContainer.addView(buildCard(it, isHistory = true)) }
    }

    private fun buildCard(mission: Mission, isHistory: Boolean): View {
        val card = PluginLayoutInflater.inflate(pluginContext, R.layout.eagle6_mission_card, null)
        card.findViewById<TextView>(R.id.card_pilot).text = mission.pilot
        card.findViewById<TextView>(R.id.card_platform).text = mission.platform
        card.findViewById<TextView>(R.id.card_type).text = mission.missionType

        val statusView = card.findViewById<TextView>(R.id.card_status)
        if (isHistory) {
            statusView.text = mission.completedAt?.let { MessageFormatter.timeStr(it) } ?: pluginContext.getString(R.string.status_complete)
            statusView.setTextColor(COLOR_GRAY)
            card.alpha = 0.65f
            card.setOnClickListener { onOpenHistory(mission) }
        } else {
            val label = MessageFormatter.statusLabel(mission.launchTimeMs, mission.expectedDurationMin)
            statusView.text = label
            statusView.setTextColor(statusColor(label))
            card.setOnClickListener { onOpenMission(mission) }
        }
        return card
    }

    private fun statusColor(label: String): Int = when {
        label.startsWith("launching") -> COLOR_AMBER
        label == pluginContext.getString(R.string.status_active) -> COLOR_GREEN
        label.startsWith("returning") -> COLOR_ORANGE
        else -> COLOR_GRAY
    }

    companion object {
        private val COLOR_AMBER = 0xFFFFD54F.toInt()
        private val COLOR_GREEN = 0xFF81C784.toInt()
        private val COLOR_ORANGE = 0xFFFF8A65.toInt()
        private val COLOR_GRAY = 0xFF90A4AE.toInt()
    }
}
