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

import kotlin.reflect.typeOf

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
            else -> value.listFromCallable()
        }?.map {
            it.mustacheLambda()?.let { lambda ->
                MapsAndListsContext(lambda, this)
            }?: (it as? () -> Any?)?.let { callable ->
                MapsAndListsContext(callable.invoke(), this)
            }?: MapsAndListsContext(it, this)
        }
    }

    override fun push(name: String, body: String?, onto: Context): Context? {
        return when (value) {
            is Map<*, *> -> value
            else -> value.mapFromCallable()
        }?.get(name)?.let { v1 ->
            body?.let {
                v1.mustacheLambda(body) ?: v1.mapFromCallable(body)
            } ?: v1.mustacheLambda() ?: v1.mapFromCallable() ?: v1
        }?.let { v2 ->
            MapsAndListsContext(v2, onto)
        }
    }

    override fun asLambda(): String? {
        return value.mustacheLambda()?.invoke()
    }
}


// TODO: there has to be a better way to handle synthetic types
private inline fun <reified T> Any?.narrowSynthetic(): T? {
    val s0 = toString()
    val s1 = typeOf<T>().toString().commonPrefixWith(s0)
    return if (s1 == s0 || s1.endsWith("<")) this as? T
    else null
}


// mustache lambdas are pushed as () -> String
private fun Any?.mustacheLambda() =
    narrowSynthetic<() -> String>()

private fun Any?.mustacheLambda(body: String) =
    narrowSynthetic<(String) -> String>()?.let {
        { it(body) }
    }

// iterable section can be pushed as List or as () -> List
private fun Any?.listFromCallable() =
    narrowSynthetic<() -> List<*>>()?.invoke()


// callable producing Map are called before pushing
private fun Any?.mapFromCallable() =
    narrowSynthetic<() -> Map<*, *>>()?.invoke()

private fun Any?.mapFromCallable(body: String) =
    narrowSynthetic<(String) -> Map<*, *>>()?.invoke(body)

