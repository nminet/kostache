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
import org.junit.jupiter.api.TestFactory
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.File
import java.nio.file.Files.createTempDirectory

class ProcessingTest : YamlTest() {

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
        mustacheKClass(template).render(data) shouldBe "123"
    }

    @Test
    fun `render a kotlin list`() {
        val template = "{{#list}}{{.}}{{/list}}"
        val data = D(list = listOf("1", "2", "3"))
        mustacheKClass(template).render(data) shouldBe "123"
    }

    @Test
    fun `render a kotlin array`() {
        val template = "{{#array}}{{.}}{{/array}}"
        val data = D(array = arrayOf("1", "2"))
        mustacheKClass(template).render(data) shouldBe "12"
    }

    @Test
    fun `render a kotlin set`() {
        val template = "{{#set}}{{.}}{{/set}}"
        val data = D(set = setOf("1", "2", "3"))
        mustacheKClass(template).render(data) shouldBe "123"
    }

    @Test
    fun `render a kotlin map`() {
        val template = "{{#table}}{{field1}}{{field2}}{{/table}}"
        val data = D(table = mapOf("field1" to "1", "field2" to "2"))
        mustacheKClass(template).render(data) shouldBe "12"
    }

    @Test
    fun `render a inner field`() {
        val template = "{{#inner}}{{string}}{{/inner}}"
        val data = D(inner = D(string = "deep"))
        mustacheKClass(template).render(data) shouldBe "deep"
    }

    @Test
    fun `render a single enum`() {
        val template = "{{.}}"
        val data = E.E1
        mustacheKClass(template).render(data) shouldBe "E1"
    }

    @Test
    fun `render a list of enums`() {
        val template = "{{#.}}>{{.}}\n{{/.}}"
        val data = listOf(E.E1, E.E3)
        mustacheKClass(template).render(data) shouldBe ">E1\n>E3\n"
    }

    @Test
    fun `enums can be used as sections`() {
        val template = "{{#.}}{{#E1}}1>{{/E1}}{{#E2}}2>{{/E2}}{{#E3}}3>{{/E3}}\n{{/.}}"
        val data = listOf(E.E1, E.E2)
        mustacheKClass(template).render(data) shouldBe "1>\n2>\n"
    }

    @Test
    fun `enums can be used as inverted sections`() {
        val template = "{{#.}}{{^E1}}1>{{/E1}}{{^E2}}2>{{/E2}}{{^E3}}3>{{/E3}}\n{{/.}}"
        val data = listOf(E.E1, E.E3)
        mustacheKClass(template).render(data) shouldBe "2>3>\n1>2>\n"
    }

    @Test
    fun `processing template with ad-hoc partials resolver`() {
        val template = "{{>p1}}{{>p2}}"
        val resolver = TemplateStore { name ->
            Template("$name says {{text}}\n")
        }
        val data = mapOf("text" to "hi")
        mustacheMapsAndLists(template, resolver).render(data) shouldBe "p1 says hi\np2 says hi\n"
    }

    @Test
    fun `processing template with partials from a folder`() {
        val template = "{{>partial1}} {{>partial2}}{{>not_found}}!"
        val directory: File = createTempDirectory("partials").toFile().also {
            File(it, "partial1.mustache").writeText("{{text1}}")
            File(it, "partial2.mustache").writeText("{{text2}}")
        }
        val resolver = TemplateFolder(directory.path)
        val data = mapOf("text1" to "hello", "text2" to "world")
        mustacheMapsAndLists(template, resolver).render(data) shouldBe "hello world!"
        directory.deleteRecursively()
    }

    @Test
    fun `updating partials in a folder requires clearing cache`() {
        val template = "{{>partial}}"
        val directory: File = createTempDirectory("partials").toFile()
        val resolver = TemplateFolder(directory.path)

        File(directory, "partial.mustache").writeText("step1")
        mustacheJson(template, resolver).render() shouldBe "step1"

        File(directory, "partial.mustache").writeText("step2")
        mustacheJson(template, resolver).render() shouldBe "step1"

        resolver.clearCache()
        mustacheJson(template, resolver).render() shouldBe "step2"

        directory.deleteRecursively()
    }

    @Test
    fun `processing template with missing partials`() {
        val template = "{{>partial}}"
        mustacheJson(template).render() shouldBe ""
    }

    @Test
    fun `lambda with no parameter is injected`() {
        val template = "{{lambda}}"
        val data = mapOf(
            "values" to listOf(1, 2, 3),
            "lambda" to { "{{#values}}{{.}}{{/values}}!" }
        )
        mustacheMapsAndLists(template).render(data) shouldBe "123!"
    }

    @Test
    fun `lambda receiving code is injected`() {
        val template = "{{#lambda}}{{#s1}}{{.}}{{/s1}}{{^s2}}{{/s2}}!{{/lambda}}"
        val data = mapOf(
            "s1" to listOf(1, 2, 3),
            "s2" to false,
            "lambda" to { code: String ->
                "--$code--"
            }
        )
        mustacheMapsAndLists(template).render(data) shouldBe "--123!--"
    }

    @Test
    fun `partial injected by lambda has access to context`() {
        val template = "{{lambda}}"
        val data = mapOf(
            "s1" to listOf(1, 2, 3),
            "lambda" to {
                "{{>p}}"
            }
        )
        val resolver = TemplateStore { _ ->
            Template("{{#s1}}{{.}}{{/s1}}")
        }
        mustacheMapsAndLists(template, resolver).render(data) shouldBe "123"
    }

    @Test
    fun `method with no parameter can be used as lambda`() {
        val template = "{{method1}}"
        val data = D(string = "hello")
        mustacheKClass(template).render(data) shouldBe "HELLO"
    }

    @Test
    fun `method with one parameter can be used as lambda`() {
        val template = "{{#d.method2}}{{text}}{{/d.method2}}"
        val data = mapOf("d" to D(string = "***"), "text" to "hi!")
        mustacheKClass(template).render(data) shouldBe "***hi!***"
    }

    @Test
    fun `json false is falsey`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": false }"""
        mustacheJson(template).render(data) shouldBe "not X"
    }

    @Test
    fun `json true is truthy`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": true }"""
        mustacheJson(template).render(data) shouldBe "X"
    }

    @Test
    fun `json null is falsey`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": null }"""
        mustacheJson(template).render(data) shouldBe "not X"
    }

    @Test
    fun `json empty list is falsey`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": [] }"""
        mustacheJson(template).render(data) shouldBe "not X"
    }

    @Test
    fun `json non-empty list`() {
        val template = "{{#x}}{{a}}{{/x}}"
        val data = """{ "x": [{ "a": 1 }, { "a": 2 }, { "a": 3 }] }"""
        mustacheJson(template).render(data) shouldBe "123"
    }

    @Test
    fun `json non-empty string is truthy`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": "false" }"""
        mustacheJson(template).render(data) shouldBe "X"
    }

    @Test
    fun `json empty string is truthy`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": "" }"""
        mustacheJson(template).render(data) shouldBe "X"
    }

    @Test
    fun `json number`() {
        val template = "{{x}}"
        val data = """{ "x": 42 }"""
        mustacheJson(template).render(data) shouldBe "42"
    }

    @Test
    fun `json null`() {
        val template = "{{x}}"
        val data = """{ "x": null }"""
        mustacheJson(template).render(data) shouldBe "null"
    }

    @Test
    fun `json true-false`() {
        val template = "{{x}}-{{y}}"
        val data = """{ "x": true, "y": false }"""
        mustacheJson(template).render(data) shouldBe "true-false"
    }

    @Test
    fun `missing tag character throws`() {
        shouldThrowExactly<IllegalStateException> {
            Template("{{")
        }
    }

    @Test
    fun `incomplete section throws`() {
        shouldThrowExactly<IllegalStateException> {
            Template("{{#xxx}}")
        }
    }

    @Test
    fun `delimiter with equals throws`() {
        shouldThrowExactly<IllegalStateException> {
            Template("{{= |= =| =}}")
        }
    }

    @Test
    fun `delimiter with whitespace throws`() {
        shouldThrowExactly<IllegalStateException> {
            Template("{{= | x | =}}")
        }
    }

    @Test
    fun `process template outside engine`() {
        val context = KClassContext("who" to "world")
        Template("hello, {{who}}!").render(context) shouldBe "hello, world!"
    }

    @Test
    fun `process yaml`() {
        val yamlLoader = Load(LoadSettings.builder().build())

        val template = "hello, {{you}}!"
        val data = yamlLoader.loadFromString("you: world")
        mustacheMapsAndLists(template).render(data) shouldBe "hello, world!"
    }

    @Test
    fun `text in a lambda section does not have to parse with default delimiters`() {
        val template = "{{=| |=}}|#lambda||x|}}|/lambda|"
        val data = mapOf(
            "lambda" to { body: String -> "--$body--" },
            "x" to "XXX"
        )
        mustacheMapsAndLists(template).render(data) shouldBe "--XXX}}--"
    }

    @Test
    fun `method returning non-String is interpolated`() {
        mustacheKClass("{{method3}}").render(D()) shouldBe "42"
    }

    @Test
    fun `lambda returning non-String is interpolated`() {
        mustacheKClass("{{lambda}}").render(D()) shouldBe "42"
    }

    @Test
    fun `lambda in class taking non-String parameter is not called`() {
        val template = "{{#lambdax}}{{string}}{{/lambdax}}"
        mustacheKClass(template).render(D(string = "good")) shouldBe "good"
    }

    @Test
    fun `lambda in Map taking non-String parameter is not called`() {
        val template = "{{#lambda}}{{x}}{{/lambda}}"
        val data = mapOf(
            "lambda" to { s: Int -> "bad: $s" },
            "x" to "good"
        )
        mustacheMapsAndLists(template).render(data) shouldBe "good"
    }

    @Test
    fun `callable returning an invalid template is interpolated as a value`() {
        val template = "{{callable}}"
        val data = mapOf(
            "callable" to { "{{x" }
        )
        mustacheMapsAndLists(template).render(data) shouldBe "{{x"
    }

    @Test
    fun `callables returning non-string are interpolated`() {
        val template = "{{#callables}}{{.}}\n{{/callables}}"
        val data = mapOf(
            "callables" to listOf( { 42 }, { null }, { listOf(1, 2, 3) })
        )
        mustacheMapsAndLists(template).render(data) shouldBe "42\nnull\n[1, 2, 3]\n"
    }

    @Test
    fun `lambda populating object without parameter`() {
        val template = "{{#lambda}}{{hi}}{{/lambda}}"
        val data = mapOf(
            "lambda" to {
                mapOf("hi" to "hi")
            }
        )
        mustacheMapsAndLists(template).render(data) shouldBe "hi"
    }

    @Test
    fun `lambda populating object according to section text`() {
        val template = "{{#lambda}}{{hi}}{{/lambda}}\n" +
                "{{#lambda}}{{FR}}{{hi}}{{/lambda}}\n"
        val data = mapOf(
            "lambda" to { text: String ->
                if (text.contains("{{FR}}")) mapOf("hi" to "bonjour")
                else mapOf("hi" to "hi")
            }
        )
        mustacheMapsAndLists(template).render(data) shouldBe "hi\nbonjour\n"
    }

    @Test
    fun `method populating object without parameter`() {
        val template = "{{#method4}}{{string}}{{/method4}}"
        val data = D()
        mustacheKClass(template).render(data) shouldBe "hello"
    }

    @Test
    fun `method populating object according to section text`() {
        val template = "{{#method5}}{{hi}}{{/method5}}\n" +
                "{{#method5}}{{FR}}{{hi}}{{/method5}}\n"
        val data = D()
        mustacheKClass(template).render(data) shouldBe "hi\nbonjour\n"
    }


    @TestFactory
    fun `additional processor tests`() =
        makeTests("processor.yml")


    private fun mustacheJson(template: String, partials: TemplateStore = emptyStore) =
        Mustache(
            template = template,
            partials = partials
        )

    private fun mustacheKClass(template: String, partials: TemplateStore = emptyStore) =
        Mustache(
            template = template,
            partials = partials,
            wrapData = ::KClassContext
        )

    private fun mustacheMapsAndLists(template: String, partials: TemplateStore = emptyStore) =
        Mustache(
            template = template,
            partials = partials,
            wrapData = ::MapsAndListsContext
        )
}
