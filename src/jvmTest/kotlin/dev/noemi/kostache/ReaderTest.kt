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

class ReaderTest {

    private fun setupReader(input: String): Reader {
        return Reader(input).apply {
            setDelimiters("{{", "}}")
        }
    }

    @Test
    fun `single line input is available`() {
        val input = " 123456 "
        val reader = setupReader(input)

        reader.readUntil("{{") shouldBe false
        reader.fetchText() shouldBe input
    }

    @Test
    fun `newlines are preserved`() {
        val input = "\n123456\r\n"
        val reader = setupReader(input)

        reader.readUntil("{{") shouldBe false
        reader.fetchText() shouldBe input
    }

    @Test
    fun `delimiters are dropped`() {
        val input = "123{{---}}456"
        val reader = setupReader(input)

        reader.readUntil("{{")
        reader.fetchText() shouldBe "123"

        reader.readUntil("}}")
        reader.readUntil("{{")
        reader.fetchText() shouldBe "456"
    }

    @Test
    fun `standalone tag is trimmed`() {
        val input = "123\n  {{# xxx }}  \n456"
        val reader = setupReader(input)

        reader.readUntil("{{")
        reader.next()
        reader.readUntil("}}")
        reader.fetchTag() shouldBe "xxx"

        reader.readUntil("{{")
        reader.fetchText() shouldBe "456"
    }

    @Test
    fun `change of delimiter applies to comment on same line`() {
        val input = "}}  [! {{x}}\n{{y}} ]  \nxxx["
        val reader = setupReader(input)

        // space before the comment is preserved
        reader.readUntil("}}")
        reader.setDelimiters("[", "]", isUpdate = true)
        reader.readUntil("[")
        reader.fetchText() shouldBe "  "

        // space after the comment is preserved
        reader.next()
        reader.readUntil("]")
        reader.readUntil("[")
        reader.fetchText() shouldBe "  \nxxx"
    }

    @Test
    fun `mixed standalone and delimiter tag on same line are standalone`() {
        val input = "  {{/s1}}{{=| |=}}|#s2|\nxxx|"
        val reader = setupReader(input)

        // skip {{/s1}}
        reader.readUntil("}}")

        // skip {{=| |=}}
        reader.readUntil("}}")
        reader.setDelimiters("|", "|", isUpdate = true)

        // skip |#s2|
        reader.readUntil("|")
        reader.readUntil("|")

        reader.readUntil("|")
        // '\n' is skipped as previous line is all standalone tags
        reader.fetchText() shouldBe "xxx"
    }

    @Test
    fun `comment-like containing single tag`() {
        val input = "=}}  {{!\n [#x] \n}}"
        val reader = setupReader(input)

        reader.readUntil("=}}")
        reader.setDelimiters("[", "]", isUpdate = true)
        reader.readUntil("[")
        reader.fetchText() shouldBe "  {{!\n"
        reader.next() shouldBe '#'
        reader.readUntil("]")
        reader.fetchTag() shouldBe "x"
        reader.readUntil("[")
        reader.fetchText() shouldBe "}}"
    }

    @Test
    fun `detect missing delimiter in standalone tag`() {
        val input = "xxx\n   {{# tag"
        val reader = setupReader(input)

        reader.readUntil("{{")
        reader.readUntil("}}") shouldBe false
    }
}
