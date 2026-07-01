package com.atakmap.android.eagle6.cot

import com.atakmap.android.eagle6.model.Mission
import com.atakmap.coremap.maps.coords.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CotBuilder {

    private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun now() = ISO.format(Date())
    private fun stale() = ISO.format(Date(System.currentTimeMillis() + 3_600_000L))

    // COT mission events are deferred pending Maven Smart System coordination.
    // groupChatEvent remains active for chat room messaging.

    fun groupChatEvent(
        message: String,
        groupName: String,
        selfUid: String,
        selfCallsign: String
    ): String {
        val now = now(); val stale = stale()
        val uid = "GeoChat.$selfUid.$groupName.${System.currentTimeMillis()}"
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<event version="2.0" uid="$uid" type="b-t-f" """)
            append("""time="$now" start="$now" stale="$stale" how="h-g-i-g-o">""")
            append("""<point lat="0.0" lon="0.0" hae="0.0" ce="9999999" le="9999999"/>""")
            append("<detail>")
            append("""<__chat parent="$groupName" groupOwner="false" chatroom="$groupName" """)
            append("""id="$groupName" senderCallsign="${selfCallsign.x()}">""")
            append("""<chatgrp uid0="$selfUid" id="$groupName"/>""")
            append("</__chat>")
            append("""<link uid="$selfUid" type="a-f-G-U-C" relation="p-p"/>""")
            append("""<remarks source="BAO.F.ATAK.$selfUid" to="$groupName" time="$now">${message.x()}</remarks>""")
            append("""<__serverdestination destinations="$groupName"/>""")
            append("</detail></event>")
        }
    }

    // XML-escape a string value
    private fun String.x() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
