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

import org.junit.jupiter.api.TestFactory

/**
 * Reference tests from https://github.com/mustache/spec
 *
 * The only change from the reference should be kotlin lambdas added in '~lambdas.yml'
 * Last updated on 25/7/2022
 */
class SpecsTest : YamlTest() {

    @TestFactory
    fun `comments tests`() =
        makeTests("specs/comments.yml")

    @TestFactory
    fun `delimiters tests`() =
        makeTests("specs/delimiters.yml")

    @TestFactory
    fun `interpolation tests`() =
        makeTests("specs/interpolation.yml")

    @TestFactory
    fun `inverted tests`() =
        makeTests("specs/inverted.yml")

    @TestFactory
    fun `partials tests`() =
        makeTests("specs/partials.yml")

    @TestFactory
    fun `sections tests`() =
        makeTests("specs/sections.yml")

    @TestFactory
    fun `inheritance tests`() =
        makeTests("specs/~inheritance.yml")

    @TestFactory
    fun `lambdas tests`() =
        makeTests("specs/~lambdas.yml")

    @TestFactory
    fun `dynamic names tests`() =
        makeTests("specs/~dynamic-names.yml")
}
