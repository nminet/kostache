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


internal interface Segment {
    fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String?
    fun substitute(blocks: Blocks) = this
}

internal typealias Segments = List<Segment>
internal typealias Blocks = List<Block>


internal class Text(
    private val text: String
) : Segment {

    override fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String {
        return if (indent.isEmpty()) text
        else text.split('\n').let { segs ->
            val first = segs.first()
            val last = segs.last()
            segs.joinToString(separator = "\n") { seg ->
                seg.indented((seg !== first || newLine) && (seg !== last || seg.isNotEmpty()), indent)
            }
        }
    }
}


private fun String.indented(needIndent: Boolean, indent: String): String {
    return if (needIndent) "$indent$this" else this
}


internal class Value(
    private val name: String,
    private val escaped: Boolean
) : Segment {

    override fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String {
        val text = context.resolve(name)?.let { resolved ->
            resolved.asLambda()?.let { text ->
                Template.load(text)?.render(context, partials, indent, newLine)
                    ?: text.indented(newLine, indent)
            } ?: resolved.asValue().indented(newLine, indent)
        } ?: "".indented(newLine, indent)
        return if (escaped) htmlEscape(text) else text
    }
}


internal class Section(
    private val name: String,
    private val isSeqCheck: Boolean,
    private val children: Segments,
    private val openDelimiter: String,
    private val closeDelimiter: String,
    private val input: String,
    private val start: Int,
    private val after: Int
) : Segment {

    override fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String? {
        val body = input.substring(start, after)
        return context.resolve(name, body)?.let { resolved ->
            if (isSeqCheck) {
                if (!resolved.push().isNullOrEmpty()) {
                    children.render(context, partials, indent, newLine)
                } else {
                    ""
                }
            } else {
                resolved.asLambda()?.let { text ->
                    val delimiters =
                        if (openDelimiter == "{{" && closeDelimiter == "}}") ""
                        else "{{=$openDelimiter $closeDelimiter=}}\n"
                    val template = delimiters + text
                    Template.load(template)?.render(context, partials, indent, newLine)
                        ?: text.indented(newLine, indent)
                } ?: resolved.push()?.joinToString(separator = "") {
                    children.render(it, partials, indent, newLine)
                } ?: resolved.ifTruthy {
                    children.render(it, partials, indent, newLine)
                }
            }
        }
    }

    override fun substitute(blocks: Blocks): Section {
        return Section(name, isSeqCheck, children.substitute(blocks), openDelimiter, closeDelimiter, input, start, after)
    }
}


internal class InvertedSection(
    private val name: String,
    private val children: Segments
) : Segment {

    override fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String? {
        return context.resolve(name).ifFalsey {
            children.render(context, partials, indent, newLine)
        }
    }

    override fun substitute(blocks: Blocks): InvertedSection {
        return InvertedSection(name, children.substitute(blocks))
    }
}


internal class Block(
    val name: String,
    private val children: Segments
) : Segment {

    override fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String {
        return children.render(context, partials, indent, newLine)
    }

    override fun substitute(blocks: Blocks): Block {
        return blocks.firstOrNull { it.name == name }
            ?: Block(name, children.substitute(blocks))
    }
}


internal class Partial(
    private val name: String,
    private val isDynamic: Boolean,
    private val indent: String,
    private val parameters: Blocks? = null
) : Segment {

    override fun render(context: Context, partials: TemplateStore, indent: String, newLine: Boolean): String {
        val partial =
            if (isDynamic) context.resolve(name)?.asValue() ?: ""
            else name
        val loaded = partials[partial]?.segments ?: return ""
        val segments = parameters?.let { loaded.substitute(it) } ?: loaded
        return segments.render(context, partials, indent + this.indent, newLine)
    }

    override fun substitute(blocks: Blocks): Partial {
        val merged = parameters?.let { ps ->
            val overrides = blocks.map(Block::name)
            ps.filter { it.name !in overrides } + blocks
        }
        return Partial(name, isDynamic, indent, merged)
    }
}


internal fun Segments.render(
    context: Context,
    partials: TemplateStore,
    indent: String,
    newLine: Boolean
): String {
    @Suppress("NAME_SHADOWING")
    var newLine = newLine
    return joinToString(separator = "") { segment ->
        segment.render(context, partials, indent, newLine)?.also {
            newLine = it.endsWith('\n')
        } ?: ""
    }
}

private fun Segments.substitute(blocks: Blocks): Segments {
    return map { it.substitute(blocks) }
}

private inline fun Context?.ifFalsey(block: () -> String): String? {
    return if (this?.isFalsey() != false) block() else null
}

private inline fun Context.ifTruthy(block: (Context) -> String): String? {
    return if (!isFalsey()) block(this) else null
}

// use the entity mapping from javascript implementation of mustache
private val entityMap = listOf(
    "&" to "&amp;",
    "<" to "&lt;",
    ">" to "&gt;",
    "\"" to "&quot;",
    "'" to "&#39;",
    "/" to "&#47;",
    "=" to "&#61;",
    "`" to "&#96;"
)

private fun htmlEscape(input: String): String {
    return entityMap.fold(input) { str, (char, entity) ->
        str.replace(char, entity)
    }
}
