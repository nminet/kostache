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

class LambdasTest {

    class Invocable(private val x: String) {
        operator fun invoke() =
            "$x$x$x {{content}} $x$x$x"

        operator fun invoke(template: String) =
            "$x$x$x $template $x$x$x"
    }

    @Suppress("unused")
    class Callables(val content: String = " ", private val x: String) {
        fun method() =
            "$x {{content}} $x"

        fun method(template: String) =
            "$x $template $x"

        val lambda1 =
            { "$x$x {{content}} $x$x" }
        val lambda2 =
            { it: String -> "$x$x $it $x$x" }

        val invocable = Invocable(x)
    }

    @Test
    fun `use method with no parameter as lambda`() {
        val template = loadForKclass("{{method}}")
        val data = Callables("X", "-")
        template.render(data) shouldBe "- X -"
    }

    @Test
    fun `use method with one parameter as lambda`() {
        val template = loadForKclass("{{#method}}{{content}}{{/method}}")
        val data = Callables("XXX", "-")
        template.render(data) shouldBe "- XXX -"
    }

    @Test
    fun `use val with no parameter as lambda`() {
        val template = loadForKclass("{{lambda1}}")
        val data = Callables("X", "|")
        template.render(data) shouldBe "|| X ||"
    }

    @Test
    fun `use val with one parameter as lambda`() {
        val template = loadForKclass("{{#lambda2}}{{content}}{{/lambda2}}")
        val data = Callables("XXX", "|")
        template.render(data) shouldBe "|| XXX ||"
    }

    @Test
    fun `use invocable field with no parameter as lambda`() {
        val template = loadForKclass("{{invocable}}")
        val data = Callables("X", "+")
        template.render(data) shouldBe "+++ X +++"
    }

    @Test
    fun `use invocable field with one parameter as lambda`() {
        val template = loadForKclass("{{#invocable}}{{content}}{{/invocable}}")
        val data = Callables("XXX", "+")
        template.render(data) shouldBe "+++ XXX +++"
    }

    @Test
    fun `lambda with no parameter in map`() {
        val template = loadForKclass("{{lambda}}")
        val data = mapOf(
            "x" to "X",
            "lambda" to { "|| {{&x}} ||" }
        )
        template.render(data) shouldBe "|| X ||"
    }

    @Test
    fun `lambda with one parameter in map`() {
        val template = loadForMapsAndLists("{{#lambda}}{{&content}}{{/lambda}}")
        val data = mapOf(
            "content" to "<->",
            "lambda" to { text: String -> "|| $text ||" }
        )
        template.render(data) shouldBe "|| <-> ||"
    }


    private fun loadForMapsAndLists(template: String) =
        Mustache(
            template = template,
            wrapData = ::MapsAndListsContext
        )


    private fun loadForKclass(template: String) =
        Mustache(
            template = template,
            wrapData = ::KClassContext
        )
}
