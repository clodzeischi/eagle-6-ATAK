package com.atakmap.android.eagle6

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.atakmap.android.dropdown.DropDown.OnStateListener
import com.atakmap.android.dropdown.DropDownReceiver
import com.atakmap.android.eagle6.chat.ChatSender
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.map.LocationPickerTool
import com.atakmap.android.eagle6.map.PlanningPreviewRenderer
import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStore
import com.atakmap.android.eagle6.settings.Eagle6PreferenceFragment
import com.atakmap.android.eagle6.ui.MissionEditView
import com.atakmap.android.eagle6.ui.MissionHistoryDetailView
import com.atakmap.android.eagle6.ui.MissionListView
import com.atakmap.android.eagle6.ui.NewMissionView
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.coremap.maps.coords.GeoPoint
import java.util.Calendar
import java.util.TimeZone

class Eagle6DropDownReceiver(
    mapView: MapView,
    private val pluginContext: Context
) : DropDownReceiver(mapView), OnStateListener {

    companion object {
        const val SHOW_PLUGIN = "com.atakmap.android.eagle6.SHOW_PLUGIN"
    }

    private val selfUid get() = mapView.selfMarker?.uid ?: "unknown"
    private val selfCallsign get() = mapView.selfMarker?.getMetaString("callsign", "UNKNOWN") ?: "UNKNOWN"
    private val chatSender get() = ChatSender(selfUid, selfCallsign)
    private val selfPoint: GeoPoint get() = mapView.selfMarker?.point ?: GeoPoint(0.0, 0.0)

    private var pickerTool: LocationPickerTool? = null
    private val navStack = ArrayDeque<android.view.View>()
    private var currentView: android.view.View? = null
    private val planningRenderer = PlanningPreviewRenderer(mapView)

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            checkMissionTimers()
            missionListView.refresh()
            tickHandler.postDelayed(this, 10_000L)
        }
    }

    // ---- Screen instances ----

    private val missionListView: MissionListView by lazy {
        MissionListView(
            pluginContext = pluginContext,
            onNewMission = { navigateTo(newMissionView.also { it.reset() }.view) },
            onOpenMission = { mission ->
                missionEditView.bind(mission)
                navigateTo(missionEditView.view)
            },
            onOpenHistory = { mission ->
                missionHistoryDetailView.bind(mission)
                navigateTo(missionHistoryDetailView.view)
            },
            onSettings = { openSettings() },
            onClearComplete = {
                MissionStore.clearCompleted()
                missionListView.refresh()
            }
        )
    }

    private val newMissionView: NewMissionView by lazy {
        NewMissionView(
            pluginContext = pluginContext,
            selfLocation = { selfPoint },
            onPickLocation = { prompt, cb -> startPicker(prompt, cb) },
            onPickLaunchTime = { cb -> showDateTimePicker(cb) },
            onConfirm = { mission -> confirmMission(mission) },
            onCancel = { onBackButtonPressed() },
            onPreviewUpdate = { launch, infilWps, activity, exfilWps, recovery ->
                planningRenderer.update(
                    launch, infilWps, activity, exfilWps, recovery,
                    Eagle6Prefs.launchZoneRadiusM.toDouble(),
                    Eagle6Prefs.activityZoneRadiusM.toDouble()
                )
            }
        )
    }

    private val missionEditView: MissionEditView by lazy {
        MissionEditView(
            pluginContext = pluginContext,
            selfLocation = { selfPoint },
            onPickLocation = { prompt, cb -> startPicker(prompt, cb) },
            onPickLaunchTime = { cb -> showDateTimePicker(cb) },
            onSave = { original, updated -> saveMission(original, updated) },
            onCancelMission = { mission -> cancelMission(mission) },
            onBack = { onBackButtonPressed() },
            onPreviewUpdate = { launch, infilWps, activity, exfilWps, recovery ->
                planningRenderer.update(
                    launch, infilWps, activity, exfilWps, recovery,
                    Eagle6Prefs.launchZoneRadiusM.toDouble(),
                    Eagle6Prefs.activityZoneRadiusM.toDouble()
                )
            }
        )
    }

    private val missionHistoryDetailView: MissionHistoryDetailView by lazy {
        MissionHistoryDetailView(
            pluginContext = pluginContext,
            onBack = { onBackButtonPressed() }
        )
    }

    // ---- DropDownReceiver ----

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SHOW_PLUGIN) {
            navStack.clear()
            currentView = missionListView.view
            missionListView.refresh()
            showDropDown(missionListView.view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this)
        }
    }

    // ---- Navigation ----

    private fun navigateTo(view: android.view.View) {
        currentView?.let { navStack.addLast(it) }
        currentView = view
        showDropDown(view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this)
    }

    override fun onBackButtonPressed(): Boolean {
        if (navStack.isNotEmpty()) {
            currentView = navStack.removeLast()
            if (navStack.isEmpty()) planningRenderer.clear()
            showDropDown(currentView!!, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this)
            return true
        }
        return false
    }

    // ---- Settings ----

    private fun openSettings() {
        closeDropDown()
        AtakBroadcast.getInstance().sendBroadcast(
            Intent("com.atakmap.app.ADVANCED_SETTINGS")
                .putExtra("toolkey", Eagle6PreferenceFragment.PREF_KEY)
        )
    }

    // ---- Map location picker ----

    private fun startPicker(prompt: String, callback: (GeoPoint) -> Unit) {
        pickerTool?.dispose()
        pickerTool = LocationPickerTool(mapView, prompt) { pt ->
            callback(pt)
            pickerTool = null
        }
        com.atakmap.android.toolbar.ToolManagerBroadcastReceiver.getInstance()
            .startTool(LocationPickerTool.TOOL_ID, null)
    }

    // ---- Date/time picker ----

    private fun showDateTimePicker(callback: (Long) -> Unit) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        DatePickerDialog(
            mapView.context,
            { _, year, month, day ->
                TimePickerDialog(
                    mapView.context,
                    { _, hour, minute ->
                        cal.set(year, month, day, hour, minute, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        callback(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ---- Mission lifecycle ----

    private fun confirmMission(mission: Mission) {
        MissionStore.add(mission)
        sendChat(MessageFormatter.plannedMessage(mission))
        goHome()
    }

    private fun saveMission(original: Mission, updated: Mission) {
        val now = System.currentTimeMillis()
        val endMs = updated.launchTimeMs + updated.expectedDurationMin * 60_000L
        if (endMs <= now) {
            val completed = updated.copy(
                launchedAt = updated.launchedAt ?: updated.launchTimeMs,
                completedAt = now
            )
            MissionStore.update(completed)
            sendChat(MessageFormatter.completeMessage(completed))
        } else {
            MissionStore.update(updated)
            sendChat(MessageFormatter.changedMessage(updated))
        }
        goHome()
    }

    private fun cancelMission(mission: Mission) {
        sendChat(MessageFormatter.cancelledMessage(mission))
        MissionStore.remove(mission.id)
        goHome()
    }

    private fun checkMissionTimers() {
        val now = System.currentTimeMillis()
        MissionStore.activeMissions.forEach { mission ->
            var current = mission

            if (current.launchedAt == null && now >= current.launchTimeMs) {
                current = current.copy(launchedAt = now)
                MissionStore.update(current)
                sendChat(MessageFormatter.launchingMessage(current))
            }

            val endMs = current.launchTimeMs + current.expectedDurationMin * 60_000L
            if (current.completedAt == null && now >= endMs) {
                current = current.copy(
                    launchedAt = current.launchedAt ?: current.launchTimeMs,
                    completedAt = now
                )
                MissionStore.update(current)
                sendChat(MessageFormatter.completeMessage(current))
            }
        }
    }

    // ---- Helpers ----

    private fun sendChat(message: String) {
        chatSender.send(message, Eagle6Prefs.chatRoomName)
    }

    private fun goHome() {
        planningRenderer.clear()
        navStack.clear()
        currentView = missionListView.view
        missionListView.refresh()
        showDropDown(missionListView.view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this)
    }

    // ---- OnStateListener ----

    override fun onDropDownVisible(v: Boolean) {
        if (v) {
            missionListView.refresh()
            tickHandler.post(tick)
        } else {
            tickHandler.removeCallbacks(tick)
        }
    }

    override fun onDropDownSelectionRemoved() {}
    override fun onDropDownSizeChanged(w: Double, h: Double) {}
    override fun onDropDownClose() { tickHandler.removeCallbacks(tick) }

    override fun disposeImpl() {
        tickHandler.removeCallbacks(tick)
        pickerTool?.dispose()
        planningRenderer.dispose()
    }
}
