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
import org.junit.jupiter.api.DynamicTest
import org.snakeyaml.engine.v2.api.ConstructNode
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.Tag
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.test.expect

open class YamlTest {

    @Suppress("UNCHECKED_CAST")
    protected fun makeTests(fileName: String) =
        readText("src/commonTest/resources", fileName)!!.let {
            yamlLoader.loadFromString(it)
        }.let { top ->
            val spec = top as Map<*, *>
            val tests = spec["tests"] as List<*>
            tests.map { inner ->
                val test = inner as Map<*, *>
                makeSingleTest(
                    name = test["name"] as String,
                    desc = test["desc"] as String,
                    template = test["template"] as String,
                    partials = test["partials"]?.let { it as Map<String, String> } ?: emptyMap(),
                    data = wrapCode(test["data"]) as Any,
                    expected = test["expected"] as String
                )
            }
        }

    private fun makeSingleTest(
        name: String,
        desc: String,
        template: String,
        partials: Map<String, String>,
        data: Any,
        expected: String
    ) = DynamicTest.dynamicTest("$name: $desc") {
        expect(expected) {
            Template(template).render(MapsAndListsContext(data), TemplateMap(partials))
        }
    }

    private fun wrapCode(data: Any?): Any? =
        when (data) {
            is Map<*, *> -> data.mapValues { (_, v) ->
                if (v is Code) {
                    if (v.arity == 0) v.toLambda0() else v.toLambda1()
                } else {
                    wrapCode(v)
                }
            }

            is List<*> -> data.map {
                wrapCode(it)
            }

            else -> {
                data
            }
        }


    private val tagConstructors = mapOf(
        Tag("!code") to ConstructNode { sourceMapNode ->
            val kotlinCodeNode = (sourceMapNode as MappingNode).value.firstOrNull {
                (it.keyNode as ScalarNode).value == "kotlin"
            }?.valueNode as ScalarNode?
            Code(kotlinCodeNode?.value ?: "{}")
        }
    )

    private val yamlLoader = Load(
        LoadSettings.builder().setTagConstructors(tagConstructors).build()
    )

    internal class Code(
        code: String
    ) {
        val arity = if (code.contains("->")) 1 else 0

        private var bindings = engine.run {
            setBindings(createBindings(), ScriptContext.ENGINE_SCOPE)
            eval("val lambda = $code")
            context.getBindings(ScriptContext.ENGINE_SCOPE)
        }

        fun toLambda0() = { evalWithBindings("lambda()") }
        fun toLambda1() = { p: String -> evalWithBindings("lambda(${escape(p)})") }

        private fun evalWithBindings(code: String) = engine.run {
            context.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
            eval(code).also {
                bindings = context.getBindings(ScriptContext.ENGINE_SCOPE)
            }.toString()
        }

        companion object {
            private val engine: ScriptEngine by lazy {
                ScriptEngineManager().getEngineByExtension("kts")
            }

            private fun escapeChar(c: Char): String =
                when (c) {
                    '\t' -> "\\t"
                    '\b' -> "\\b"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '"' -> "\\\""
                    '\\' -> "\\\\"
                    '\$' -> "\\\$"
                    in ' '..'~' -> c.toString()
                    else -> "\\u" + c.code.toString(16).padStart(4, '0')
                }

            private fun escape(string: String) =
                "\"${string.map(Companion::escapeChar).joinToString("")}\""
        }
    }
}
