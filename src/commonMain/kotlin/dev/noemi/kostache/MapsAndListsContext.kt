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

class MapsAndListsContext(
    value: Any?,
    parent: Context? = null
) : Context(value, parent) {

    override fun isFalsey(): Boolean {
        return value == null
                || (value is Boolean) && !value
                || (value is List<*>) && value.isEmpty()
    }

    override fun push(): List<Context>? {
        return when (value) {
            is List<*> -> value
            else -> null
        }?.mapNotNull {
            MapsAndListsContext(it, this)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun push(name: String, body: String?, onto: Context): Context? {
        return when (value) {
            is Map<*, *> -> value
            else -> null
        }?.get(name)?.let { v1 ->
            body?.let {
                when (val callable = (v1 as? (Any?) -> Any?)) {
                    is Function1<*, *> ->
                        try {
                            // create a new lambda capturing the result of call
                            val result = callable.invoke(body)
                            val lambda = { result }
                            lambda
                        } catch (e: ClassCastException) {
                            // the function parameter is not a String
                            null
                        }

                    else -> null
                }
            } ?: v1
        }?.let { v2 ->
            MapsAndListsContext(v2, onto)
        }
    }

    override fun asLambda(): String? {
        return when (value) {
            is Function0<*> -> when (val result = value.invoke()) {
                is String -> result
                else -> null
            }

            else -> null
        }
    }
}
