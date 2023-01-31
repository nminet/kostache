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

import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties


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

private fun property(obj: Any, name: String, body: String?): Any? {
    val kClass = obj::class
    return if (kClass.simpleName == null) null // shortcut kotlin synthetic classes
    else kClass.memberProperties.firstOrNull { it.name == name }?.call(obj)?.let { child ->
        invokeToLambdaOrContext(child, body)
            ?: lambdaToLambdaOrContext(child, body)
            ?: invokeToLambdaOrContext(child)
            ?: lambdaToLambdaOrContext(child)
            ?: child
    } ?: kClass.memberFunctions.filter { it.name == name }.let { methods ->
        val method1 = methods.firstOrNull { it.hasOneStringParameter() }
        val method0 = methods.firstOrNull { it.hasNoParameter() }
        when {
            body != null && method1 != null && method1.returnsString() -> {
                { method1.call(obj, body) }
            }

            body != null && method1 != null -> {
                method1.call(obj, body)
            }

            method0 != null && method0.returnsString() -> {
                { method0.call(obj) }
            }

            method0 != null -> {
                method0.call(obj)
            }

            else -> null
        }
    } ?: kClass.memberFunctions.filter { it.name == "invoke" }.let { methods ->
        val method1 = methods.firstOrNull { it.hasOneStringParameter() }
        val method0 = methods.firstOrNull { it.hasNoParameter() }
        when {
            method1 != null && body != null -> method1.call(obj, body)
            method0 != null -> method0.call(obj)
            else -> null
        }.child(name, body)
    }
}


private fun KFunction<*>.returnsString(): Boolean {
    return returnType == stringKType
}

private fun KFunction<*>.hasNoParameter(): Boolean {
    return parameters.size == 1
}

private fun KFunction<*>.hasOneStringParameter(): Boolean {
    return parameters.size == 2 && parameters[1].type == stringKType
}

private val stringKType = String::class.createType()


@Suppress("UNCHECKED_CAST")
private fun invokeToLambdaOrContext(obj: Any, body: String?): Any? {
    return body?.let {
        try {
            obj::class.memberFunctions.firstOrNull {
                it.name == "invoke" && it.hasOneStringParameter()
            }?.let {
                if (it.returnsString()) {
                    { (it as KFunction<String>).call(obj, body) }
                } else {
                    it.call(obj, body)
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun lambdaToLambdaOrContext(obj: Any?, body: String?): Any? {
    return body?.let {
        val s = obj.toString()
        if (s == "(kotlin.String) -> kotlin.String")
            (obj as (String) -> String).let { lambda ->
                { lambda(body) }
            }
        else if (s.startsWith("(kotlin.String) -> "))
            (obj as (String) -> Any?).invoke(body)
        else
            null
    }
}

@Suppress("UNCHECKED_CAST")
private fun invokeToLambdaOrContext(obj: Any): Any? {
    return try {
        obj::class.memberFunctions.firstOrNull {
            it.name == "invoke" && it.hasNoParameter()
        }?.let {
            if (it.returnsString()) {
                { (it as KFunction<String>).call(obj) }
            } else {
                it.call(obj)
            }
        }
    } catch (_: Exception) {
        null
    }
}

@Suppress("UNCHECKED_CAST")
private fun lambdaToLambdaOrContext(obj: Any?): Any? {
    val s = obj.toString()
    return if (s == "() -> kotlin.String")
        (obj as () -> String).let { lambda ->
            { lambda() }
        }
    else if (s.startsWith("() -> "))
        (obj as () -> Any?).invoke()
    else
        null
}
