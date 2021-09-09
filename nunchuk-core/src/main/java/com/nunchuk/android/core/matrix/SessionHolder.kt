package com.nunchuk.android.core.matrix

import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room

object SessionHolder {
    var activeSession: Session? = null

    fun storeActiveSession(session: Session) {
        session.apply {
            activeSession = this
            open()
            startSync(false)
        }
    }

    fun hasActiveSession() = activeSession != null

    var currentRoom: Room? = null
}
