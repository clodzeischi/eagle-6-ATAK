package com.atakmap.android.eagle6

import android.content.Context
import android.content.Intent
import com.atakmap.android.cot.detail.CotDetailManager
import com.atakmap.android.dropdown.DropDownMapComponent
import com.atakmap.android.eagle6.chat.ChatRoomManager
import com.atakmap.android.eagle6.cot.Eagle6DetailHandler
import com.atakmap.android.eagle6.map.ReceivedMissionRenderer
import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.eagle6.model.MissionStore
import com.atakmap.android.eagle6.settings.Eagle6PreferenceFragment
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter
import com.atakmap.android.maps.MapView
import com.atakmap.app.preferences.ToolsPreferenceFragment

class Eagle6MapComponent : DropDownMapComponent() {

    private lateinit var dropDown: Eagle6DropDownReceiver
    private lateinit var detailHandler: Eagle6DetailHandler
    private lateinit var renderer: ReceivedMissionRenderer

    override fun onCreate(context: Context, intent: Intent, view: MapView) {
        context.setTheme(com.atakmap.android.plugintemplate.plugin.R.style.ATAKPluginTheme)
        Eagle6Prefs.init(context)
        MissionStore.init(view.context)
        super.onCreate(context, intent, view)
        ChatRoomManager.ensureRoom(Eagle6Prefs.chatRoomName)
        Eagle6Prefs.selfCallsign = view.selfMarker?.getMetaString("callsign", "UNKNOWN") ?: "UNKNOWN"

        // Drop-down receiver
        dropDown = Eagle6DropDownReceiver(view, context)
        val filter = DocumentedIntentFilter()
        filter.addAction(Eagle6DropDownReceiver.SHOW_PLUGIN, "Show Eagle-6 plugin")
        registerDropDownReceiver(dropDown, filter)

        // COT detail handler for received __eagle-detail elements
        renderer = ReceivedMissionRenderer(view)
        detailHandler = Eagle6DetailHandler(view).also { it.renderer = renderer }
        CotDetailManager.getInstance().registerHandler(detailHandler)

        // Register in ATAK's Tools > Preferences menu
        ToolsPreferenceFragment.register(
            ToolsPreferenceFragment.ToolPreference(
                "Eagle-6 Preferences",
                "Mission management for UAS operations",
                Eagle6PreferenceFragment.PREF_KEY,
                null,
                Eagle6PreferenceFragment(context)
            )
        )
    }

    override fun onDestroyImpl(context: Context, view: MapView) {
        CotDetailManager.getInstance().unregisterHandler(detailHandler)
        super.onDestroyImpl(context, view)
        renderer.dispose()
        ToolsPreferenceFragment.unregister(Eagle6PreferenceFragment.PREF_KEY)
    }
}
