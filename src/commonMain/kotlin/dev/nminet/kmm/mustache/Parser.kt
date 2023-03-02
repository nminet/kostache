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


internal class Parser(
    private val reader: Reader,
    private var openDelimiter: String,
    private var closeDelimiter: String,
    private val sectionTag: String? = null
) {
    fun process(): Segments {
        reader.setDelimiters(openDelimiter, closeDelimiter)
        while (readText() && readTag()) Unit
        return segments
    }

    private fun readText(): Boolean {
        return reader.readUntil(openDelimiter).also {
            check(it || sectionTag == null) { "unexpected end of input" }
            reader.setMark()
            reader.fetchText()?.let { text ->
                segments.add(
                    Text(text)
                )
            }
        }
    }

    private fun readTag(): Boolean {
        return when (reader.next()) {
            null -> unexpectedEndOfInput()
            '/' -> checkEndOfSection()
            '!' -> checkComment()
            '=' -> setDelimiters()
            '#' -> addSection()
            '^' -> addInvertedSection()
            '$' -> addBlock()
            '>' -> addPartial(isParent = false)
            '<' -> addPartial(isParent = true)
            '{' -> addValue(escaped = false, curlies = true)
            '&' -> addValue(escaped = false, curlies = false)
            else -> {
                reader.back()
                addValue(escaped = true, curlies = false)
            }
        }
    }

    private fun unexpectedEndOfInput(): Nothing {
        throw IllegalStateException("unexpected end of input")
    }

    private fun checkEndOfSection(): Boolean {
        processTag { name ->
            check(name == sectionTag) { "unexpected end of section" }
        }
        return false
    }

    private fun checkComment(): Boolean {
        return reader.readUntil(closeDelimiter).also {
            check(it) { "missing delimiter" }
        }
    }

    private fun addSection(): Boolean {
        return addSegment { name ->
            val start = reader.pos
            val children = parseTag(name)
            Section(name, children, openDelimiter, closeDelimiter, reader.input, start, reader.mark)
        }
    }

    private fun addInvertedSection(): Boolean {
        return addSegment { name ->
            InvertedSection(name, parseTag(name))
        }
    }

    private fun addBlock(): Boolean {
        return addSegment { name ->
            Block(name, parseTag(name))
        }
    }

    private fun addPartial(isParent: Boolean): Boolean {
        return addSegment { tag ->
            val isDynamic = tag.startsWith('*')
            val name = if (isDynamic) tag.drop(1).trimStart() else tag
            val parameters = if (isParent) parseTag(name).filterIsInstance<Block>() else null
            Partial(name, isDynamic, reader.indent, parameters)
        }
    }

    private fun addValue(escaped: Boolean, curlies: Boolean): Boolean {
        val delimiter = if (curlies) "}$closeDelimiter" else closeDelimiter
        return addSegment(delimiter) { name ->
            Value(name, escaped)
        }
    }

    private fun setDelimiters(): Boolean {
        return processTag("=$closeDelimiter") {
            check(it.matches(Regex("""[^\s=]+\s+[^\s=]+"""))) { "invalid delimiters section" }
            openDelimiter = it.substringBefore(" ")
            closeDelimiter = it.substringAfterLast(" ")
            reader.setDelimiters(openDelimiter, closeDelimiter, isUpdate = true)
        }
    }

    private fun processTag(delimiter: String = closeDelimiter, process: (String) -> Unit): Boolean {
        return reader.readUntil(delimiter).also {
            check(it) { "missing delimiter" }
            reader.fetchTag()?.let { tag ->
                process(tag)
            } ?: throw IllegalStateException("missing tag")
        }
    }

    private fun addSegment(delimiter: String = closeDelimiter, makeSegment: (String) -> Segment): Boolean {
        return processTag(delimiter) { tag ->
            segments.add(makeSegment(tag))
        }
    }

    private fun parseTag(name: String): Segments {
        return Parser(reader, openDelimiter, closeDelimiter, name).process()
    }

    private val segments = mutableListOf<Segment>()
}
