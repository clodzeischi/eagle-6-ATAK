package com.atakmap.android.eagle6.chat

import com.atakmap.android.cot.CotMapComponent
import com.atakmap.android.eagle6.cot.CotBuilder
import com.atakmap.coremap.cot.event.CotEvent

class ChatSender(
    private val selfUid: String,
    private val selfCallsign: String
) {
    fun send(message: String, roomName: String) {
        val xml = CotBuilder.groupChatEvent(message, roomName, selfUid, selfCallsign)
        CotMapComponent.getInternalDispatcher().dispatch(CotEvent.parse(xml))
    }
}
