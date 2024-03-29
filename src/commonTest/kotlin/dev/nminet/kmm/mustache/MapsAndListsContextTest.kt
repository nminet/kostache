/*
 * Copyright (c) 2023 Noel MINET
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.nminet.kmm.mustache

import io.kotest.matchers.shouldBe
import kotlin.test.Test


class MapsAndListsContextTest {

    @Test
    fun `create view from map`() {
        val view = MapsAndListsContext(
            mapOf("a" to 1)
        )

        val v1 = view.push("a")
        v1?.asValue() shouldBe "1"
    }

    @Test
    fun `create view from list with one element`() {
        val view = MapsAndListsContext(
            listOf(
                mapOf("a" to 1)
            )
        )

        val v1 = view.push()!!

        v1.size shouldBe 1
        v1[0].asValue() shouldBe "{a=1}"
    }

    @Test
    fun `create view from list with two elements`() {
        val view = MapsAndListsContext(
            listOf(
                mapOf("a" to 1),
                mapOf("b" to 2)
            )
        )

        val v1 = view.push()!!

        v1.size shouldBe 2
        v1[0].asValue() shouldBe "{a=1}"
        v1[1].asValue() shouldBe "{b=2}"
    }

    @Test
    fun `get empty list from a map`() {
        val view = MapsAndListsContext(
            mapOf(
                "a" to listOf<String>()
            )
        )

        view.isFalsey() shouldBe false
        view.push() shouldBe null

        val v1 = view.push("a")!!
        v1.isFalsey() shouldBe true
        v1.asValue() shouldBe "[]"
    }

    @Test
    fun `get item from a list in a map`() {
        val view = MapsAndListsContext(
            mapOf(
                "a" to listOf(
                    "a1" to 1,
                    "a2" to 2
                )
            )
        )

        view.push() shouldBe null

        val v1 = view.push("a")!!
        val v2 = v1.push()!!
        v2.size shouldBe 2
        v2[0].asValue() shouldBe "(a1, 1)"
        v2[1].asValue() shouldBe "(a2, 2)"
    }

    @Test
    fun `resolve in current`() {
        val view = MapsAndListsContext(
            mapOf(
                "aa" to "A",
                "ab" to mapOf(
                    "ba" to "B",
                    "bb" to mapOf(
                        "ca" to "C",
                        "cb" to listOf<String>()
                    )
                )
            )
        )

        val v1 = view.push("ab")!!
        v1.resolve("ba")?.asValue() shouldBe "B"
        v1.resolve("aa")?.asValue() shouldBe "A"
    }

    @Test
    fun `broken chain should be null`() {
        val view = MapsAndListsContext(
            mapOf(
                "a" to mapOf(
                    "b" to mapOf<String, String>()
                ),
                "c" to mapOf(
                    "name" to "Jim"
                )
            )
        )

        view.resolve("a.b.c.name")?.asValue() shouldBe null
    }

    @Test
    fun `lambda returning a Map uses the output as new context`() {
        val ctx = MapsAndListsContext(
            { mapOf("a" to "hello") }
        )

        ctx.resolve("a")?.asValue() shouldBe "hello"
    }

    @Test
    fun `lambda returning a List uses the output as new context`() {
        val ctx = MapsAndListsContext(
            { listOf("e1", "e2") }
        )

        val view = ctx.push()!!
        view.size shouldBe 2
        view[0].asValue() shouldBe "e1"
        view[1].asValue() shouldBe "e2"
    }

    @Test
    fun `list of lambdas returning String act as list of mustache lambdas`() {
        val ctx = MapsAndListsContext(
            listOf({ "{{x}}" }, { "{{y}}" })
        )

        val view = ctx.push()!!
        view.size shouldBe 2
        view[0].asLambda() shouldBe "{{x}}"
        view[1].asLambda() shouldBe "{{y}}"
    }
}
