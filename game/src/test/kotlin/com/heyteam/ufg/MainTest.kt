package com.heyteam.ufg

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class MainTest :
    FunSpec({
        test("addition should work") {
            2 + 2 shouldBe 4
        }
        test("string should start with Hello") {
            "Hello, World!" shouldStartWith "Hello"
        }
    })
