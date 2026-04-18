package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.entity.Character

interface CharacterRepository {
    fun findById(id: Int): Character?

    fun findAll(): List<Character>
}
