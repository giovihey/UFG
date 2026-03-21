package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.entity.World

interface RenderPort {
    fun render(world: World)
}
