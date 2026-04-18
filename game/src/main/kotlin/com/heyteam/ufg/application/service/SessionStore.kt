package com.heyteam.ufg.application.service

import com.heyteam.ufg.domain.component.Session

class SessionStore {
    private var session: Session? = null

    fun save(s: Session) {
        session = s
    }

    fun get(): Session? = session

    fun clear() {
        session = null
    }
}
