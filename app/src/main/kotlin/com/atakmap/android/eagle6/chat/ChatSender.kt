package com.atakmap.android.eagle6.chat

import com.atakmap.android.cot.CotMapComponent
import com.atakmap.android.eagle6.cot.CotBuilder
import com.atakmap.coremap.cot.event.CotEvent

class ChatSender(
    private val selfUid: String,
    private val selfCallsign: String
) {
    fun sendToRooms(message: String, rooms: List<String>) {
        if (rooms.isEmpty()) return
        val dispatcher = CotMapComponent.getInternalDispatcher()
        rooms.forEach { room ->
            val xml = CotBuilder.groupChatEvent(message, room, selfUid, selfCallsign)
            dispatcher.dispatch(CotEvent.parse(xml))
        }
    }
}
