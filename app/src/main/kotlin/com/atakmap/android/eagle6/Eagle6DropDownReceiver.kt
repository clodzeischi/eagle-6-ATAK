package com.atakmap.android.eagle6

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.atakmap.android.cot.CotMapComponent
import com.atakmap.android.dropdown.DropDown.OnStateListener
import com.atakmap.android.dropdown.DropDownReceiver
import com.atakmap.android.eagle6.chat.ChatSender
import com.atakmap.android.eagle6.cot.CotBuilder
import com.atakmap.android.eagle6.cot.MessageFormatter
import com.atakmap.android.eagle6.map.LocationPickerTool
import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.android.eagle6.model.MissionStore
import com.atakmap.android.eagle6.ui.LandingView
import com.atakmap.android.eagle6.ui.MissionDetailView
import com.atakmap.android.eagle6.ui.MissionListView
import com.atakmap.android.eagle6.settings.Eagle6PreferenceFragment
import com.atakmap.android.eagle6.ui.NewMissionView
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.coremap.cot.event.CotEvent
import com.atakmap.coremap.maps.coords.GeoPoint

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

    // Active location picker tool – one at a time
    private var pickerTool: LocationPickerTool? = null

    // Navigation back-stack (views that were showing before the current one)
    private val navStack = ArrayDeque<android.view.View>()
    private var currentView: android.view.View? = null

    // ---- Lazy screen instances ----

    private val missionListView: MissionListView by lazy {
        MissionListView(
            pluginContext = pluginContext,
            onNewMission = { navigateTo(newMissionView.also { it.refresh() }.view) },
            onOpenMission = { mission ->
                missionDetailView.bind(mission)
                navigateTo(missionDetailView.view)
            },
            onSettings = { openSettings() }
        )
    }

    private val newMissionView: NewMissionView by lazy {
        NewMissionView(
            pluginContext = pluginContext,
            selfLocation = { selfPoint },
            onPickLocation = { prompt, cb -> startPicker(prompt, cb) },
            onLaunch = { mission -> launchMission(mission) },
            onCancel = { onBackButtonPressed() }
        )
    }

    private val missionDetailView: MissionDetailView by lazy {
        MissionDetailView(
            pluginContext = pluginContext,
            onPickLocation = { prompt, cb -> startPicker(prompt, cb) },
            onMissionUpdated = { mission, message -> onMissionStatusChanged(mission, message) },
            onRth = { mission ->
                landingView.bind(mission)
                navigateTo(landingView.view)
            }
        )
    }

    private val landingView: LandingView by lazy {
        LandingView(
            pluginContext = pluginContext,
            selfLocation = { selfPoint },
            onPickLocation = { prompt, cb -> startPicker(prompt, cb) },
            onLanded = { mission, landingPoint -> completeLanding(mission, landingPoint) }
        )
    }

    // ---- DropDownReceiver ----

    private fun dispatchReceive(context: Context, intent: Intent) {
        if (intent.action == SHOW_PLUGIN) {
            navStack.clear()
            currentView = missionListView.view
            missionListView.refresh()
            showDropDown(
                missionListView.view,
                HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT,
                false, this
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) = dispatchReceive(context, intent)

    // ---- Navigation ----

    private fun navigateTo(view: android.view.View) {
        currentView?.let { navStack.addLast(it) }
        currentView = view
        showDropDown(view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this)
    }

    override fun onBackButtonPressed(): Boolean {
        if (navStack.isNotEmpty()) {
            currentView = navStack.removeLast()
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

    // ---- Mission lifecycle ----

    private fun launchMission(mission: Mission) {
        MissionStore.add(mission)

        val launchMgrs = MessageFormatter.toMgrs(mission.launchLocation)
        val actMgrs = MessageFormatter.toMgrs(mission.activityLocation)
        val message = MessageFormatter.launchMessage(
            mission.pilot, mission.platform, mission.missionType,
            launchMgrs, actMgrs, mission.altitudeFt
        )
        dispatchCot(mission, mission.launchLocation, message, "LAUNCH")
        chatSender.sendToRooms(message, Eagle6Prefs.chatRooms)
        logEntry(message)

        missionDetailView.bind(mission)
        navigateTo(missionDetailView.view)
    }

    private fun onMissionStatusChanged(mission: Mission, message: String) {
        MissionStore.update(mission)
        val location = mission.activityLocation
        val tag = mission.status.name
        dispatchCot(mission, location, message, tag)
        chatSender.sendToRooms(message, Eagle6Prefs.chatRooms)
        logEntry(message)
    }

    private fun completeLanding(mission: Mission, landingPoint: GeoPoint) {
        val landMgrs = MessageFormatter.toMgrs(landingPoint)
        val message = MessageFormatter.landMessage(
            mission.pilot, mission.platform, landMgrs, mission.missionType
        )
        dispatchCot(mission, landingPoint, message, "LAND")
        chatSender.sendToRooms(message, Eagle6Prefs.chatRooms)
        logEntry(message)

        MissionStore.remove(mission.id)

        // Back to mission list
        navStack.clear()
        currentView = missionListView.view
        missionListView.refresh()
        showDropDown(missionListView.view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this)

        Toast.makeText(pluginContext, "Mission complete.", Toast.LENGTH_SHORT).show()
    }

    // ---- COT dispatch ----

    private fun dispatchCot(mission: Mission, location: GeoPoint, message: String, tag: String) {
        val xml = CotBuilder.missionEvent(mission, location, message, tag, selfUid)
        CotMapComponent.getInternalDispatcher().dispatch(CotEvent.parse(xml))
    }

    // ---- Local log ----

    private fun logEntry(message: String) {
        try {
            val logFile = java.io.File(pluginContext.filesDir, "eagle6.log")
            logFile.appendText("$message\n")
        } catch (e: Exception) {
            com.atakmap.coremap.log.Log.w("Eagle6", "Log write failed: ${e.message}")
        }
    }

    // ---- OnStateListener ----
    override fun onDropDownSelectionRemoved() {}
    override fun onDropDownVisible(v: Boolean) { if (v) missionListView.refresh() }
    override fun onDropDownSizeChanged(width: Double, height: Double) {}
    override fun onDropDownClose() {}

    override fun disposeImpl() {
        pickerTool?.dispose()
    }
}
