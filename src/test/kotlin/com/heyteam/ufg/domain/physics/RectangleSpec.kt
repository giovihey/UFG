package com.heyteam.ufg.domain.physics

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RectangleSpec :
    StringSpec({
        "overlaps returns true when rectangles intersect" {
            val a = Rectangle(0.0, 0.0, 10.0, 10.0)
            val b = Rectangle(5.0, 5.0, 10.0, 10.0)

            a.overlaps(b) shouldBe true
            b.overlaps(a) shouldBe true
        }

        "overlaps returns false when rectangles are separate" {
            val a = Rectangle(0.0, 0.0, 10.0, 10.0)
            val b = Rectangle(11.0, 0.0, 5.0, 5.0)

            a.overlaps(b) shouldBe false
            b.overlaps(a) shouldBe false
        }

        "overlaps returns false when rectangles only touch at edges or corners" {
            val a = Rectangle(0.0, 0.0, 10.0, 10.0)
            val touchSide = Rectangle(10.0, 0.0, 5.0, 5.0)
            val touchCorner = Rectangle(10.0, 10.0, 5.0, 5.0)

            a.overlaps(touchSide) shouldBe false
            a.overlaps(touchCorner) shouldBe false
        }
    })
