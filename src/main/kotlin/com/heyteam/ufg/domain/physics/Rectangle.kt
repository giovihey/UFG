package com.heyteam.ufg.domain.physics

data class Rectangle(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    fun overlaps(other: Rectangle): Boolean {
        val thisRight = x + width
        val thisBottom = y + height
        val otherRight = other.x + other.width
        val otherBottom = other.y + other.height

        return thisRight > other.x &&
            otherRight > x &&
            thisBottom > other.y &&
            otherBottom > y
    }
}
