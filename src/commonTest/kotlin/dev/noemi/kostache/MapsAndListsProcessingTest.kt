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

import dev.noemi.kostache.testing.TestFiles
import io.kotest.matchers.shouldBe
import kotlin.test.Test


class MapsAndListsProcessingTest {

    @Test
    fun `callable returning an invalid template is interpolated as a value`() {
        val template = "{{callable}}"
        val data = mapOf(
            "callable" to { "{{x" }
        )
        mustache(template).render(data) shouldBe "{{x"
    }

    @Test
    fun `callables returning non-string are interpolated`() {
        val template = "{{#callables}}{{.}}\n{{/callables}}"
        val data = mapOf(
            "callables" to listOf({ 42 }, { null }, { listOf(1, 2, 3) })
        )
        mustache(template).render(data) shouldBe "42\n\n[1, 2, 3]\n"
    }

    @Test
    fun `lambda populating object without parameter`() {
        val template = "{{#lambda}}{{hi}}{{/lambda}}"
        val data = mapOf(
            "lambda" to {
                mapOf("hi" to "hi")
            }
        )
        mustache(template).render(data) shouldBe "hi"
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
        mustache(template).render(data) shouldBe "hi\nbonjour\n"
    }

    @Test
    fun `lambda with no parameter is injected`() {
        val template = "{{lambda}}"
        val data = mapOf(
            "values" to listOf(1, 2, 3),
            "lambda" to { "{{#values}}{{.}}{{/values}}!" }
        )
        mustache(template).render(data) shouldBe "123!"
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
        mustache(template).render(data) shouldBe "--123!--"
    }

    @Test
    fun `lambda with one parameter in map`() {
        val template = "{{#lambda}}{{&content}}{{/lambda}}"
        val data = mapOf(
            "content" to "<->",
            "lambda" to { text: String -> "|| $text ||" }
        )
        mustache(template).render(data) shouldBe "|| <-> ||"
    }

    @Test
    fun `processing template with ad-hoc partials resolver`() {
        val template = "{{>p1}}{{>p2}}"
        val resolver = TemplateStore { name ->
            Template("$name says {{text}}\n")
        }
        val data = mapOf("text" to "hi")
        mustache(template, resolver).render(data) shouldBe "p1 says hi\np2 says hi\n"
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
        mustache(template, resolver).render(data) shouldBe "123"
    }

    @Test
    fun `processing with partials from a folder`() {
        TestFiles().use {
            val template = "{{>partial1}} {{>partial2}}{{>not_found}}!"
            val resolver = TemplateFolder(it.dirname)

            it.writeFile("partial1.mustache", "{{text1}}")
            it.writeFile("partial2.mustache", "{{text2}}")

            val data = mapOf("text1" to "hello", "text2" to "world")
            mustache(template, resolver).render(data) shouldBe "hello world!"
        }
    }

    @Test
    fun `updating partials in a folder requires clearing cache`() {
        TestFiles().use {
            val template = "{{>partial}}"
            val resolver = TemplateFolder(it.dirname)
            val mustache = mustache(template, resolver)

            it.writeFile("partial.mustache", "step1")
            mustache.render() shouldBe "step1"

            it.writeFile("partial.mustache", "step2")
            mustache.render() shouldBe "step1"

            resolver.clearCache()
            mustache.render() shouldBe "step2"
        }
    }


    private fun mustache(template: String, partials: TemplateStore = emptyStore) =
        Mustache(
            template = template,
            partials = partials,
            wrapData = ::MapsAndListsContext
        )
}
