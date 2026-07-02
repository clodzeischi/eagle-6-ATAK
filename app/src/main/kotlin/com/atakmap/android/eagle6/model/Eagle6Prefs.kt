package com.atakmap.android.eagle6.model

import android.content.Context
import android.content.SharedPreferences
import com.atakmap.android.preference.AtakPreferences
import androidx.core.content.edit

object Eagle6Prefs {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = AtakPreferences.getInstance(context).sharedPrefs
    }

    // -- Identity --
    var selfCallsign: String
        get() = prefs.getString(KEY_SELF_CALLSIGN, "UNKNOWN") ?: "UNKNOWN"
        set(value) { prefs.edit { putString(KEY_SELF_CALLSIGN, value) } }

    // -- Config lists (saved by Settings screen on Save) --
    var pilots: List<String>
        get() = getList(KEY_PILOTS, listOf(selfCallsign))
        set(value) { setList(KEY_PILOTS, value) }

    var platforms: List<String>
        get() = getList(KEY_PLATFORMS, DEFAULT_PLATFORMS)
        set(value) { setList(KEY_PLATFORMS, value) }

    var missionTypes: List<String>
        get() = getList(KEY_MISSION_TYPES, DEFAULT_MISSION_TYPES)
        set(value) { setList(KEY_MISSION_TYPES, value) }

    var altitudes: List<String>
        get() = getList(KEY_ALTITUDES, DEFAULT_ALTITUDES)
        set(value) { setList(KEY_ALTITUDES, value) }

    var launchZoneRadiusM: Int
        get() = prefs.getInt(KEY_LAUNCH_RADIUS, 50)
        set(value) { prefs.edit(commit = true) {
            putInt(
                KEY_LAUNCH_RADIUS,
                value.coerceIn(10, 100)
            )
        } }

    var activityZoneRadiusM: Int
        get() = prefs.getInt(KEY_ACTIVITY_RADIUS, 300)
        set(value) { prefs.edit(commit = true) {
            putInt(
                KEY_ACTIVITY_RADIUS,
                value.coerceIn(100, 1000)
            )
        } }

    var useZuluTime: Boolean
        get() = prefs.getBoolean(KEY_USE_ZULU_TIME, true)
        set(value) { prefs.edit(commit = true) { putBoolean(KEY_USE_ZULU_TIME, value) } }

    var chatRoomName: String
        get() = prefs.getString(KEY_CHAT_ROOM_NAME, DEFAULT_CHAT_ROOM) ?: DEFAULT_CHAT_ROOM
        set(value) { prefs.edit(commit = true) { putString(KEY_CHAT_ROOM_NAME, value) } }

    // -- Last selections: read on New Mission load, written only on Launch --
    val lastPilotIndex: Int get() = prefs.getInt(KEY_LAST_PILOT, 0)
    val lastPlatformIndex: Int get() = prefs.getInt(KEY_LAST_PLATFORM, 0)
    val lastMissionTypeIndex: Int get() = prefs.getInt(KEY_LAST_MISSION_TYPE, 0)
    val lastAltitudeIndex: Int get() = prefs.getInt(KEY_LAST_ALTITUDE, 0)
    val lastDurationMin: Int get() = prefs.getInt(KEY_LAST_DURATION, 0)

    fun saveLastSelections(
        pilotIdx: Int,
        platformIdx: Int,
        missionTypeIdx: Int,
        altitudeIdx: Int,
        durationMin: Int
    ) {
        prefs.edit(commit = true) {
            putInt(KEY_LAST_PILOT, pilotIdx)
                .putInt(KEY_LAST_PLATFORM, platformIdx)
                .putInt(KEY_LAST_MISSION_TYPE, missionTypeIdx)
                .putInt(KEY_LAST_ALTITUDE, altitudeIdx)
                .putInt(KEY_LAST_DURATION, durationMin)
        }
    }

    private fun getList(key: String, default: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return default
        return if (raw.isBlank()) default else raw.split(DELIM)
    }

    private fun setList(key: String, list: List<String>) {
        prefs.edit(commit = true) { putString(key, list.joinToString(DELIM)) }
    }

    // ASCII Unit Separator — cannot be typed by a user
    private val DELIM = ""

    private const val KEY_SELF_CALLSIGN = "eagle6_self_callsign"
    const val KEY_PILOTS = "eagle6_pilots"
    const val KEY_PLATFORMS = "eagle6_platforms"
    const val KEY_MISSION_TYPES = "eagle6_mission_types"
    const val KEY_ALTITUDES = "eagle6_altitudes"
    const val KEY_LAUNCH_RADIUS = "eagle6_launch_radius"
    const val KEY_ACTIVITY_RADIUS = "eagle6_activity_radius"
    private const val KEY_USE_ZULU_TIME = "eagle6_use_zulu_time"
    const val KEY_CHAT_ROOM_NAME = "eagle6_chat_room_name"
    const val DEFAULT_CHAT_ROOM = "uas-coordination"
    private const val KEY_LAST_PILOT = "eagle6_last_pilot_idx"
    private const val KEY_LAST_PLATFORM = "eagle6_last_platform_idx"
    private const val KEY_LAST_MISSION_TYPE = "eagle6_last_mission_type_idx"
    private const val KEY_LAST_ALTITUDE = "eagle6_last_altitude_idx"
    private const val KEY_LAST_DURATION = "eagle6_last_duration_min"

    val DEFAULT_PLATFORMS = listOf("GENERIC-UAS")
    val DEFAULT_MISSION_TYPES = listOf("SEARCH")
    val DEFAULT_ALTITUDES = listOf("400")
}
