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

import kotlinx.serialization.json.*


class KotlinxJsonContext(
    value: Any?,
    parent: Context? = null
) : Context(value, parent) {

    override fun isFalsey(): Boolean {
        return value == null
                || value is JsonNull
                || (value is JsonPrimitive) && !value.isString && value.content == "false"
                || (value is JsonArray) && value.isEmpty()
    }

    override fun push(): List<Context>? {
        return (value as? JsonArray)?.map {
            KotlinxJsonContext(it, this)
        }
    }

    override fun push(name: String, body: String?, onto: Context): Context? {
        return (value as? JsonObject)?.get(name)?.let {
            KotlinxJsonContext(it, onto)
        }
    }

    override fun asValue(): String {
        return when {
            value is JsonPrimitive && value.isString -> value.content
            value is JsonNull -> ""
            else -> value.toString()
        }
    }


    companion object {
        val wrap = { data: Any? ->
            val element = when (data) {
                is JsonElement -> data
                is String -> Json.parseToJsonElement(data)
                else -> null
            }
            KotlinxJsonContext(element)
        }

        inline val <reified T> T.asJsonElement: JsonElement
            get() = Json.encodeToJsonElement(this)
    }
}
