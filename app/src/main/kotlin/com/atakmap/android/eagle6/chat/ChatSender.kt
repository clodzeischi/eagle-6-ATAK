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
        val event = CotEvent.parse(xml)
        CotMapComponent.getExternalDispatcher().dispatch(event) // sends to TAK server + network
        CotMapComponent.getInternalDispatcher().dispatch(event) // shows on local device UI
    }
}
