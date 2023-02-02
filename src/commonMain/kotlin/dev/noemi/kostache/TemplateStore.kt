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

import dev.noemi.kostache.expects.readText


fun interface TemplateStore {
    fun resolve(name: String): Template
}


class TemplateFolder(
    private val path: String,
    private val extension: String = "mustache"
) : TemplateStore {

    override fun resolve(name: String): Template {
        if (name !in templates)
            load(name)
        return templates[name] ?: Template()
    }

    fun clearCache() {
        templates.clear()
    }

    private fun load(name: String) {
        templates[name] = readText(path, "$name.$extension")?.let {
            Template(it)
        } ?: Template()
    }

    private val templates = mutableMapOf<String, Template>()
}


class TemplateMap(sourceMap: Map<String, String>) : TemplateStore {

    private val templates = sourceMap.mapValues { (_, template) ->
        Template(template)
    }

    override fun resolve(name: String): Template {
        return templates[name] ?: Template()
    }
}


val emptyStore = TemplateStore { _ -> Template() }
