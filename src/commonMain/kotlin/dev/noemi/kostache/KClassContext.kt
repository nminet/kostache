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


internal expect fun property(obj: Any, name: String, body: String?): Any?

class KClassContext(
    value: Any?,
    parent: Context? = null
) : Context(value, parent) {

    override fun isFalsey(): Boolean {
        return value == null
                || (value is Boolean) && !value
                || (value is List<*>) && value.isEmpty()
                || (value is Set<*>) && value.isEmpty()
                || (value is Array<*>) && value.isEmpty()
    }

    override fun push(): List<Context>? {
        return when (value) {
            is List<*> -> value
            is Set<*> -> value
            is Array<*> -> value.toList()
            else -> null
        }?.map {
            KClassContext(it, this)
        }
    }

    override fun push(name: String, body: String?, onto: Context): Context? {
        return value.child(name, body)?.let {
            KClassContext(it, onto)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun asLambda(): String? {
        return (value as? () -> String)?.invoke()
    }
}

internal fun Any?.child(name: String, body: String?): Any? {
    return when (this) {
        null -> null
        is Map<*, *> -> get(name)
        is Map.Entry<*, *> -> if (key == name) value else null
        is Pair<*, *> -> if (first == name) second else null
        is Enum<*> -> if (toString() == name) name else null
        else -> property(this, name, body)
    }
}
