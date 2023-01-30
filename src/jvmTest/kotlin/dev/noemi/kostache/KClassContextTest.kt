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

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KClassContextTest {

    class Test1(
        val v: Int
    )

    @Test
    fun `missing field does not produce new context`() {
        val ctx = KClassContext(Test1(v = 10))

        ctx.push("x") shouldBe null
    }

    @Test
    fun `field value is converted to String`() {
        val ctx = KClassContext(Test1(v = 42))

        val view = ctx.push("v")!!
        view.asValue() shouldBe  "42"
    }

    class Test2 {
        fun m1() = 42
    }

    @Test
    fun `method with no parameter is called before render`() {
        val ctx = KClassContext(Test2())

        val view = ctx.push("m1")!!
        view.asValue() shouldBe  "42"
    }

    class Test3 {
        fun m1() = "-"
        fun m1(x: String) = "-$x-"
    }

    @Test
    fun `method matching mustache lambda with body is prioritized`() {
        val ctx = KClassContext(Test3())

        val view = ctx.push("m1", "X", ctx)
        view?.asLambda() shouldBe  "-X-"
    }

    class Test4() {
        operator fun invoke() = mapOf("x" to "y")
        operator fun invoke(s: String) = mapOf("x" to s)
    }

    @Test
    fun `invoke can produce a context`() {
        val ctx = KClassContext(Test4())

        val view = ctx.push("x", null, ctx)
        view?.asValue() shouldBe "y"
    }

    @Test
    fun `invoke with parameter can produce a context`() {
        val ctx = KClassContext(Test4())

        val view = ctx.push("x", "y", ctx)
        view?.asValue() shouldBe "y"
    }
}
