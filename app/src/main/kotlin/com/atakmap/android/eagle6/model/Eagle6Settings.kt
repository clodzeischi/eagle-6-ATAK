package com.atakmap.android.eagle6.model

import android.content.Context
import android.content.SharedPreferences

class Eagle6Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selfCallsign: String
        get() = prefs.getString(KEY_SELF_CALLSIGN, "UNKNOWN") ?: "UNKNOWN"
        set(value) = prefs.edit().putString(KEY_SELF_CALLSIGN, value).apply()

    var pilots: List<String>
        get() = getList(KEY_PILOTS, listOf(selfCallsign))
        set(value) = setList(KEY_PILOTS, value)

    var platforms: List<String>
        get() = getList(KEY_PLATFORMS, DEFAULT_PLATFORMS)
        set(value) = setList(KEY_PLATFORMS, value)

    var missionTypes: List<String>
        get() = getList(KEY_MISSION_TYPES, DEFAULT_MISSION_TYPES)
        set(value) = setList(KEY_MISSION_TYPES, value)

    var altitudes: List<String>
        get() = getList(KEY_ALTITUDES, DEFAULT_ALTITUDES)
        set(value) = setList(KEY_ALTITUDES, value)

    var launchZoneRadiusM: Int
        get() = prefs.getInt(KEY_LAUNCH_RADIUS, 50)
        set(value) = prefs.edit().putInt(KEY_LAUNCH_RADIUS, value.coerceIn(10, 100)).apply()

    var activityZoneRadiusM: Int
        get() = prefs.getInt(KEY_ACTIVITY_RADIUS, 300)
        set(value) = prefs.edit().putInt(KEY_ACTIVITY_RADIUS, value.coerceIn(100, 1000)).apply()

    var chatRooms: List<String>
        get() = getList(KEY_CHAT_ROOMS, emptyList())
        set(value) = setList(KEY_CHAT_ROOMS, value)

    // Last-used form values (persist between sessions)
    var lastPilotIndex: Int
        get() = prefs.getInt(KEY_LAST_PILOT, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_PILOT, value).apply()

    var lastPlatformIndex: Int
        get() = prefs.getInt(KEY_LAST_PLATFORM, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_PLATFORM, value).apply()

    var lastMissionTypeIndex: Int
        get() = prefs.getInt(KEY_LAST_MISSION_TYPE, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_MISSION_TYPE, value).apply()

    var lastAltitudeIndex: Int
        get() = prefs.getInt(KEY_LAST_ALTITUDE, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_ALTITUDE, value).apply()

    var lastDurationMin: Int
        get() = prefs.getInt(KEY_LAST_DURATION, 60)
        set(value) = prefs.edit().putInt(KEY_LAST_DURATION, value).apply()

    private fun getList(key: String, default: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return default
        return if (raw.isBlank()) default else raw.split(DELIM)
    }

    private fun setList(key: String, list: List<String>) {
        prefs.edit().putString(key, list.joinToString(DELIM)).apply()
    }

    companion object {
        const val PREFS_NAME = "eagle6"
        private const val DELIM = "||"

        private const val KEY_SELF_CALLSIGN = "self_callsign"
        const val KEY_PILOTS = "pilots"
        const val KEY_PLATFORMS = "platforms"
        const val KEY_MISSION_TYPES = "mission_types"
        const val KEY_ALTITUDES = "altitudes"
        const val KEY_LAUNCH_RADIUS = "launch_zone_radius"
        const val KEY_ACTIVITY_RADIUS = "activity_zone_radius"
        const val KEY_CHAT_ROOMS = "chat_rooms"
        private const val KEY_LAST_PILOT = "last_pilot_idx"
        private const val KEY_LAST_PLATFORM = "last_platform_idx"
        private const val KEY_LAST_MISSION_TYPE = "last_mission_type_idx"
        private const val KEY_LAST_ALTITUDE = "last_altitude_idx"
        private const val KEY_LAST_DURATION = "last_duration_min"

        val DEFAULT_PLATFORMS = listOf("GENERIC-UAS")
        val DEFAULT_MISSION_TYPES = listOf("SEARCH")
        val DEFAULT_ALTITUDES = listOf("400")
    }
}
