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
 * A Mustache engine.
 *
 * The engine captures [template] along with the environment required for rendering:
 * [partials] provides the mapping of partial names to templates.
 * [wrap] is called when the input data to [render] is not a [Context].
 */
class Mustache(
    private val template: Template,
    private val partials: TemplateStore = emptyStore,
    private val wrap: (Any?) -> Context = KotlinxJsonContext.wrap
) {
    /**
     * Create a Mustache processor from source and set its rendering environment.
     */
    constructor(
        template: String,
        partials: TemplateStore = emptyStore,
        wrapData: (Any?) -> Context = KotlinxJsonContext.wrap
    ) : this(
        Template(template), partials, wrapData
    )

    /**
     * Feed [data] into a template, producing a [String].
     */
    fun render(data: Any? = null): String {
        return template.render(
            if (data is Context) data else wrap(data),
            partials
        )
    }
}


/**
 * A compiled Mustache template.
 */
class Template internal constructor(
    internal val segments: Segments = emptyList()
) {
    /**
     * Create a Template from source.
     *
     * If [template] is not well-formed [IllegalStateException] is raised.
     */
    constructor(template: String) : this(
        Parser(
            Reader(template),
            openDelimiter = "{{",
            closeDelimiter = "}}"
        ).process()
    )

    /**
     * Feed [context] in a template.
     *
     * [partials] provides the mapping of partial names to templates.
     * [indent] is added in front of all template lines.
     * [newLine] indicates if the template should start a new line.
     * As per Mustache specification, no indentation is added inside interpolated values
     * containing EOLs.
     */
    fun render(
        context: Context,
        partials: TemplateStore = emptyStore,
        indent: String = "",
        newLine: Boolean = true
    ): String {
        return segments.render(context, partials, indent, newLine)
    }


    companion object {

        /**
         * Try constructing a mustache template from source.
         *
         * If [template] is well-formed, return the corresponding instance,
         * otherwise return null.
         */
        fun load(template: String): Template? {
            return try {
                Template(template)
            } catch (_: IllegalStateException) {
                null
            }
        }

        /**
         * Create an empty [Template] instance.
         */
        fun empty(): Template {
            return Template()
        }
    }
}
