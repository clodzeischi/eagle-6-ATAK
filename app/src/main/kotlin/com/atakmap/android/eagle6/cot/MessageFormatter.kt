package com.atakmap.android.eagle6.cot

import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.eagle6.model.Mission
import com.atakmap.coremap.conversions.CoordinateFormat
import com.atakmap.coremap.conversions.CoordinateFormatUtilities
import com.atakmap.coremap.maps.coords.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MessageFormatter {

    private fun tz(): TimeZone =
        if (Eagle6Prefs.useZuluTime) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()

    private fun suffix(): String = if (Eagle6Prefs.useZuluTime) "Z" else "J"

    private fun timeFmt() = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz() }
    private fun displayFmt() = SimpleDateFormat("dd MMM HH:mm", Locale.US).apply { timeZone = tz() }

    fun timeStr(): String = "${timeFmt().format(Date())}${suffix()}"
    fun timeStr(ms: Long): String = "${timeFmt().format(Date(ms))}${suffix()}"
    fun displayTime(ms: Long): String = "${displayFmt().format(Date(ms))}${suffix()}"

    fun toMgrs(point: GeoPoint): String =
        CoordinateFormatUtilities.formatToShortString(point, CoordinateFormat.MGRS)

    // ---- Mission event messages ----

    fun plannedMessage(mission: Mission): String =
        "${timeStr()}: ${mission.pilot} planned ${mission.platform} ${mission.missionType} " +
        "at ${toMgrs(mission.activityLocation)}, launching at ${timeStr(mission.launchTimeMs)}."

    fun launchingMessage(mission: Mission): String =
        "${timeStr()}: ${mission.pilot} launching ${mission.platform} ${mission.missionType} " +
        "at ${toMgrs(mission.activityLocation)}."

    fun completeMessage(mission: Mission): String =
        "${timeStr()}: ${mission.pilot} ${mission.platform} ${mission.missionType} complete."

    fun cancelledMessage(mission: Mission): String =
        "${timeStr()}: ${mission.pilot} ${timeStr(mission.launchTimeMs)} ${mission.missionType} cancelled."

    fun changedMessage(mission: Mission): String =
        "${timeStr()}: ${mission.pilot} changed ${mission.platform} ${mission.missionType} " +
        "to ${toMgrs(mission.activityLocation)}, launching at ${timeStr(mission.launchTimeMs)}."

    // ---- Status label for mission cards ----

    fun statusLabel(launchTimeMs: Long, durationMin: Int): String {
        val now = System.currentTimeMillis()
        val endMs = launchTimeMs + durationMin * 60_000L
        val warnMs = endMs - 10 * 60_000L
        return when {
            now < launchTimeMs - 60 * 60_000L -> "launching at ${timeStr(launchTimeMs)}"
            now < launchTimeMs -> "launching in ${ceilMin(launchTimeMs - now)} min"
            now < warnMs -> "active"
            now < endMs -> "returning in ${ceilMin(endMs - now)} min"
            else -> "complete"
        }
    }

    private fun ceilMin(ms: Long): Long = (ms + 59_000L) / 60_000L
}
