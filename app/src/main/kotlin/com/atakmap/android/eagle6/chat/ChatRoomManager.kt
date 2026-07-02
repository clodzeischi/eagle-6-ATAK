package com.atakmap.android.eagle6.chat

import com.atakmap.android.contact.Contacts
import com.atakmap.android.contact.GroupContact

object ChatRoomManager {
    fun ensureRoom(roomName: String) {
        val contacts = Contacts.getInstance() ?: return
        if (contacts.getContactByUuid(roomName) == null) {
            contacts.addContact(GroupContact(roomName, roomName, true))
        }
    }
}
