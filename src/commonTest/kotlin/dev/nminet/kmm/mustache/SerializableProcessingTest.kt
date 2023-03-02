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

import dev.nminet.kmm.mustache.KotlinxJsonContext.Companion.asJsonElement
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SerializableProcessingTest {

    @Test
    fun `render serializable`() {
        val template = "{{label}}\n{{#wigs}}{{field1}} = {{field2}}\n{{/wigs}}"
        val data = Gadget(
            label = "hello",
            wigs = listOf(
                Widget("1", 1),
                Widget("2", 2)
            )
        )
        Mustache(template).render(data.asJsonElement) shouldBe "hello\n1 = 1\n2 = 2\n"
    }
}


@Serializable
data class Widget(
    val field1: String,
    val field2: Int
)

@Serializable
data class Gadget(
    val label: String,
    val wigs: List<Widget>
)
