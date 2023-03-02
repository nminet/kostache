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

import dev.nminet.kmm.mustache.testing.YamlTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

class YamlProcessingTest : YamlTest() {

    @Test
    fun `process yaml`() {
        val yamlLoader = Load(LoadSettings.builder().build())

        val template = "hello, {{you}}!"
        val data = yamlLoader.loadFromString("you: world")
        mustache(template).render(data) shouldBe "hello, world!"
    }

    @Test
    fun `text in a lambda section does not have to parse with default delimiters`() {
        val template = "{{=| |=}}|#lambda||x|}}|/lambda|"
        val data = mapOf(
            "lambda" to { body: String -> "--$body--" },
            "x" to "XXX"
        )
        mustache(template).render(data) shouldBe "--XXX}}--"
    }


    @TestFactory
    fun `additional processor tests`() =
        makeTests("processor.yml")


    private fun mustache(template: String) =
        Mustache(
            template = template,
            wrapData = ::MapsAndListsContext
        )
}
