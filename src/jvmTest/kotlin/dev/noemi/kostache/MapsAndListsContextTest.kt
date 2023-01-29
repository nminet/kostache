/*
 * Copyright (c) 2022 Noel MINET
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

package dev.noemi.kostache

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings


class MapsAndListsContextTest {

    @Test
    fun `create view from list with one element`() {
        val view = root("[a: 1]")

        val v1 = view.push()!!

        v1.size shouldBe 1
        v1[0].asValue() shouldBe yamlNode("a: 1")
    }

    @Test
    fun `create view from list with two elements`() {
        val view = root("[a: 1, b: 2]")

        val v1 = view.push()!!

        v1.size shouldBe 2
        v1[0].asValue() shouldBe yamlNode("a: 1")
        v1[1].asValue() shouldBe yamlNode("b: 2")
    }

    @Test
    fun `get empty list from a map`() {
        val view = root("{a: []}")

        view.isFalsey() shouldBe false
        view.push() shouldBe null

        val v1 = view.push("a")!!
        v1.isFalsey() shouldBe true
        v1.asValue() shouldBe yamlNode("[]")
    }

    @Test
    fun `get item from a list in a map`() {
        val view = root("{a: [a1: 1, a2: 2]}")

        view.push() shouldBe null

        val v1 = view.push("a")!!
        val v2 = v1.push()!!
        v2.size shouldBe 2
        v2[0].asValue() shouldBe yamlNode("a1: 1")
        v2[1].asValue() shouldBe yamlNode("a2: 2")
    }

    @Test
    fun `resolve in current`() {
        val view = root("{aa: A, ab: {ba: B, bb: {ca: C, cb: []}}}")

        val v1 = view.push("ab")!!
        v1.resolve("ba")?.asValue() shouldBe "B"
        v1.resolve("aa")?.asValue() shouldBe "A"
    }

    @Test
    fun `broken chain should be null`() {
        val view = root("{a: {b: {}}, c: { name: 'Jim'}}")

        view.resolve("a.b.c.name")?.asValue() shouldBe null
    }

    @Test
    fun `lambda returning a Map uses the output as new context`() {
        val ctx = MapsAndListsContext(
            { mapOf("a" to "A") }
        )

        ctx.resolve("a")?.asValue() shouldBe "A"
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
    fun `list of lamdbas returning String as as mustache lambdas`(){
        val ctx = MapsAndListsContext(
            listOf({ "{{x}}" }, { "{{y}}" })
        )

        val view = ctx.push()!!
        view.size shouldBe 2
        view[0].asLambda() shouldBe "{{x}}"
        view[1].asLambda() shouldBe "{{y}}"
    }


    companion object {
        private val yamlLoader = Load(
            LoadSettings.builder().build()
        )

        private fun yamlNode(text: String) =
            yamlLoader.loadFromString(text).toString()

        private fun root(text: String) =
            MapsAndListsContext(yamlLoader.loadFromString(text))
    }
}
