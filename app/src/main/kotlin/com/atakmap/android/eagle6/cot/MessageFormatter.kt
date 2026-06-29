package com.atakmap.android.eagle6.cot

import com.atakmap.coremap.conversions.CoordinateFormat
import com.atakmap.coremap.conversions.CoordinateFormatUtilities
import com.atakmap.coremap.maps.coords.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MessageFormatter {

    private val TIME_FMT = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun timeStr(): String = "${TIME_FMT.format(Date())}Z"

    fun toMgrs(point: GeoPoint): String =
        CoordinateFormatUtilities.formatToShortString(point, CoordinateFormat.MGRS)

    fun launchMessage(
        pilot: String, platform: String, missionType: String,
        launchMgrs: String, activityMgrs: String, altFt: String
    ): String =
        "${timeStr()}: $pilot launched $platform from $launchMgrs to conduct ${missionType.lowercase()} at $activityMgrs from $altFt' AGL."

    fun onTaskMessage(pilot: String): String =
        "${timeStr()}: $pilot is ON-TASK."

    fun retaskTypeMessage(pilot: String, newType: String): String =
        "${timeStr()}: $pilot retasked to $newType."

    fun retaskLocationMessage(pilot: String, newMgrs: String, altFt: String): String =
        "${timeStr()}: $pilot retasked to $newMgrs, $altFt' AGL."

    fun landMessage(pilot: String, platform: String, landMgrs: String, missionType: String): String =
        "${timeStr()}: $pilot landed $platform at $landMgrs. ${missionType.uppercase()} complete."
}
