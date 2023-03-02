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

/**
 * A Reader that ignores whitespace surrounding standalone tags.
 *
 * The reader establishes a window on complete lines ignoring EOL in comments.
 */
internal class Reader(
    val input: String
) {
    var pos = 0
        private set

    var mark = 0
        private set

    fun setMark() {
        mark = pos - openDelimiter.length
    }

    val indent: String
        get() = input.substring(windowStart, textStart)

    fun readUntil(delimiter: String): Boolean {
        sb.clear()
        do {
            val c = next() ?: return false
            sb.append(c)
            if (sb.endsWith(delimiter)) {
                sb.setLength(sb.length - delimiter.length)
                return true
            }
        } while (true)
    }

    fun fetchText(): String? {
        return if (sb.isNotEmpty()) sb.toString() else null
    }

    fun fetchTag(): String? {
        return fetchText()?.trim()
    }

    private val sb = StringBuilder()

    fun next(): Char? {
        if (pos == textAfter) {
            if (windowAfter < input.length)
                nextWindow()
            else
                return null
        }
        return input[pos++]
    }

    fun back() {
        pos--
    }

    fun setDelimiters(openDelimiter: String, closeDelimiter: String, isUpdate: Boolean = false) {
        val previous =
            if (isUpdate) Pair(this.openDelimiter, this.closeDelimiter)
            else null

        this.openDelimiter = openDelimiter
        this.closeDelimiter = closeDelimiter

        // recover EOLs that may have been caught as multi-line comment with old delimiters
        if (previous != null && textAfter == windowAfter) {
            windowAfter = spanWindow(pos)
            val notStandalone = input.substring(windowStart, pos).trimStart()
                .split(previous.first).drop(1).any { !it.endsWith(previous.second) }
            val standaloneCloseDelimiter =
                if (notStandalone || !isStandaloneOpen(pos, windowAfter)) null
                else findStandaloneCloseDelimiter(pos + openDelimiter.length, windowAfter)
            textAfter = standaloneCloseDelimiter?.let { it + closeDelimiter.length } ?: windowAfter
        }
    }

    private fun nextWindow() {
        windowStart = windowAfter
        windowAfter = spanWindow(windowStart)
        val (start, after) = trimStandalone(windowStart, windowAfter)
        textStart = start
        textAfter = after
        pos = start
    }

    private fun spanWindow(start: Int): Int {
        // locate first EOL not in a comment
        var after = start
        var inComment = false
        do {
            val c = if (after < input.length) input[after++] else break
            if (!inComment && c == commentSigil) {
                inComment = endsWith("$openDelimiter$commentSigil", after)
            }
            if (inComment && endsWith(closeDelimiter, after)) {
                inComment = false
            }
        } while (inComment || c != '\n')
        return after
    }

    private fun trimStandalone(start: Int, after: Int): Pair<Int, Int> {
        return findStandaloneOpenDelimiter(start, after)?.let { pos1 ->
            findStandaloneCloseDelimiter(pos1 + openDelimiter.length + 1, after)?.let { pos2 ->
                Pair(pos1, pos2 + closeDelimiter.length)
            }
        } ?: Pair(start, after)
    }

    private fun endsWith(suffix: String, after: Int): Boolean {
        val idx = after - suffix.length
        return input.indexOf(suffix, idx) == idx
    }

    private fun findStandaloneOpenDelimiter(start: Int, after: Int): Int? {
        val idx = spanWhitespace(start, after)
        val found = isStandaloneOpen(idx, after)
        return if (found) idx else null
    }

    private fun isStandaloneOpen(idx: Int, after: Int): Boolean {
        return input.indexOf(openDelimiter, idx) == idx
                && idx + openDelimiter.length < after
                && input[idx + openDelimiter.length] in strippableSigils
    }

    private fun findStandaloneCloseDelimiter(start: Int, after: Int): Int? {
        var idx = input.indexOf(closeDelimiter, start)
        while (idx >= 0 && isStandaloneOpen(idx + closeDelimiter.length, after)) {
            idx = input.indexOf(closeDelimiter, idx + closeDelimiter.length)
        }
        val found = (idx >= 0)
                && spanWhitespace(idx + closeDelimiter.length, after) == after
        return if (found) idx else null
    }

    private fun spanWhitespace(start: Int, after: Int): Int {
        var idx = start
        while (idx < after && input[idx].isWhitespace()) idx++
        return idx
    }


    private lateinit var openDelimiter: String
    private lateinit var closeDelimiter: String

    private var windowStart = 0
    private var windowAfter = 0
    private var textStart = 0
    private var textAfter = 0

    companion object {
        private const val strippableSigils = "#^/>=!$<"
        private const val commentSigil = '!'
    }
}
