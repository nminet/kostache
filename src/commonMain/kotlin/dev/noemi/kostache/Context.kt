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


/**
 * Base class for data processing into a Mustache Template.
 *
 * Derived classes must implement
 * - [isFalsey]: indicate if the context renders as a regular or inverted section.
 * - [push]: for iterable add list of child contexts; for object add named child.
 *
 * Derived classes can override
 * - [asLambda]: retrieve result of mustache lambda.
 * - [asValue]: retrieve the text to emit on render.
 */
abstract class Context(
    val value: Any?,
    val parent: Context?
) {
    abstract fun isFalsey(): Boolean
    abstract fun push(): List<Context>?
    abstract fun push(name: String, body: String? = null, onto: Context = this): Context?

    open fun asLambda(): String? {
        return null
    }

    open fun asValue(): String {
        return value.toString()
    }


    fun resolve(name: String, body: String? = null, down: Context? = parent, onto: Context = this): Context? {
        return when {
            name == "." -> this
            name.contains(".") -> name.split(".", limit = 2).let { (head, tail) ->
                resolve(head, down = down)?.resolve(tail, body, down = null)
            }
            else -> push(name, body, onto) ?: down?.resolve(name, body, onto = onto)
        }
    }
}
