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

package dev.noemi.kostache

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KClassProcessingTest {

    @Suppress("unused")
    data class D(
        val string: String = "",
        val list: List<String> = emptyList(),
        val array: Array<String> = arrayOf("", ""),
        val set: Set<String> = emptySet(),
        val table: Map<String, String> = emptyMap(),
        val inner: D? = null,
        val lambda: () -> Any = { 42 },
        val lambdax: (Int) -> Any = { x -> x }
    ) {
        fun method1() = string.uppercase()
        fun method2(text: String) = "$string$text$string"
        fun method3() = 42
        fun method4() = D(string = "hello")
        fun method5(text: String): Map<String, String> {
            return if (text.contains("{{FR}}")) mapOf("hi" to "bonjour")
            else mapOf("hi" to "hi")
        }
    }

    enum class E { E1, E2, E3 }


    @Test
    fun `render a kotlin string`() {
        val template = "{{string}}"
        val data = D(string = "123")
        mustache(template).render(data) shouldBe "123"
    }

    @Test
    fun `render a kotlin list`() {
        val template = "{{#list}}{{.}}{{/list}}"
        val data = D(list = listOf("1", "2", "3"))
        mustache(template).render(data) shouldBe "123"
    }

    @Test
    fun `render a kotlin array`() {
        val template = "{{#array}}{{.}}{{/array}}"
        val data = D(array = arrayOf("1", "2"))
        mustache(template).render(data) shouldBe "12"
    }

    @Test
    fun `render a kotlin set`() {
        val template = "{{#set}}{{.}}{{/set}}"
        val data = D(set = setOf("1", "2", "3"))
        mustache(template).render(data) shouldBe "123"
    }

    @Test
    fun `render a kotlin map`() {
        val template = "{{#table}}{{field1}}{{field2}}{{/table}}"
        val data = D(table = mapOf("field1" to "1", "field2" to "2"))
        mustache(template).render(data) shouldBe "12"
    }

    @Test
    fun `render a inner field`() {
        val template = "{{#inner}}{{string}}{{/inner}}"
        val data = D(inner = D(string = "deep"))
        mustache(template).render(data) shouldBe "deep"
    }

    @Test
    fun `render a single enum`() {
        val template = "{{.}}"
        val data = E.E1
        mustache(template).render(data) shouldBe "E1"
    }

    @Test
    fun `render a list of enums`() {
        val template = "{{#.}}>{{.}}\n{{/.}}"
        val data = listOf(E.E1, E.E3)
        mustache(template).render(data) shouldBe ">E1\n>E3\n"
    }

    @Test
    fun `enums can be used as sections`() {
        val template = "{{#.}}{{#E1}}1>{{/E1}}{{#E2}}2>{{/E2}}{{#E3}}3>{{/E3}}\n{{/.}}"
        val data = listOf(E.E1, E.E2)
        mustache(template).render(data) shouldBe "1>\n2>\n"
    }

    @Test
    fun `enums can be used as inverted sections`() {
        val template = "{{#.}}{{^E1}}1>{{/E1}}{{^E2}}2>{{/E2}}{{^E3}}3>{{/E3}}\n{{/.}}"
        val data = listOf(E.E1, E.E3)
        mustache(template).render(data) shouldBe "2>3>\n1>2>\n"
    }

    @Test
    fun `processing template with missing partials`() {
        val template = "{{>partial}}"
        Mustache(template).render() shouldBe ""
    }

    @Test
    fun `method with no parameter can be used as lambda`() {
        val template = "{{method1}}"
        val data = D(string = "hello")
        mustache(template).render(data) shouldBe "HELLO"
    }

    @Test
    fun `method with one parameter can be used as lambda`() {
        val template = "{{#d.method2}}{{text}}{{/d.method2}}"
        val data = mapOf("d" to D(string = "***"), "text" to "hi!")
        mustache(template).render(data) shouldBe "***hi!***"
    }

    @Test
    fun `method returning non-String is interpolated`() {
        mustache("{{method3}}").render(D()) shouldBe "42"
    }

    @Test
    fun `lambda returning non-String is interpolated`() {
        mustache("{{lambda}}").render(D()) shouldBe "42"
    }

    @Test
    fun `lambda in class taking non-String parameter is not called`() {
        val template = "{{#lambdax}}{{string}}{{/lambdax}}"
        mustache(template).render(D(string = "good")) shouldBe "good"
    }
    @Test
    fun `method populating object without parameter`() {
        val template = "{{#method4}}{{string}}{{/method4}}"
        val data = D()
        mustache(template).render(data) shouldBe "hello"
    }

    @Test
    fun `process template outside engine`() {
        val context = KClassContext("who" to "world")
        Template("hello, {{who}}!").render(context) shouldBe "hello, world!"
    }

    @Test
    fun `method populating object according to section text`() {
        val template = "{{#method5}}{{hi}}{{/method5}}\n" +
                "{{#method5}}{{FR}}{{hi}}{{/method5}}\n"
        val data = D()
        mustache(template).render(data) shouldBe "hi\nbonjour\n"
    }

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
        val template = "{{method}}"
        val data = Callables("X", "-")
        mustache(template).render(data) shouldBe "- X -"
    }

    @Test
    fun `use method with one parameter as lambda`() {
        val template = "{{#method}}{{content}}{{/method}}"
        val data = Callables("XXX", "-")
        mustache(template).render(data) shouldBe "- XXX -"
    }

    @Test
    fun `use val with no parameter as lambda`() {
        val template = "{{lambda1}}"
        val data = Callables("X", "|")
        mustache(template).render(data) shouldBe "|| X ||"
    }

    @Test
    fun `use val with one parameter as lambda`() {
        val template = "{{#lambda2}}{{content}}{{/lambda2}}"
        val data = Callables("XXX", "|")
        mustache(template).render(data) shouldBe "|| XXX ||"
    }

    @Test
    fun `use invocable field with no parameter as lambda`() {
        val template = "{{invocable}}"
        val data = Callables("X", "+")
        mustache(template).render(data) shouldBe "+++ X +++"
    }

    @Test
    fun `use invocable field with one parameter as lambda`() {
        val template = "{{#invocable}}{{content}}{{/invocable}}"
        val data = Callables("XXX", "+")
        mustache(template).render(data) shouldBe "+++ XXX +++"
    }

    @Test
    fun `lambda with no parameter in map`() {
        val template = "{{lambda}}"
        val data = mapOf(
            "x" to "X",
            "lambda" to { "|| {{&x}} ||" }
        )
        mustache(template).render(data) shouldBe "|| X ||"
    }


    private fun mustache(template: String, partials: TemplateStore = emptyStore) =
        Mustache(
            template = template,
            partials = partials,
            wrapData = ::KClassContext
        )
}
