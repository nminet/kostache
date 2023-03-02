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

import io.kotest.matchers.shouldBe
import kotlin.test.Test


class JsonProcessingTest {

    @Test
    fun `json false is falsey`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": false }"""
        Mustache(template).render(data) shouldBe "not X"
    }

    @Test
    fun `json true is truthy`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": true }"""
        Mustache(template).render(data) shouldBe "X"
    }

    @Test
    fun `json null is falsey`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": null }"""
        Mustache(template).render(data) shouldBe "not X"
    }

    @Test
    fun `json empty list is falsey`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": [] }"""
        Mustache(template).render(data) shouldBe "not X"
    }

    @Test
    fun `json non-empty list`() {
        val template = "{{#x}}{{a}}{{/x}}"
        val data = """{ "x": [{ "a": 1 }, { "a": 2 }, { "a": 3 }] }"""
        Mustache(template).render(data) shouldBe "123"
    }

    @Test
    fun `json non-empty string is truthy`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": "false" }"""
        Mustache(template).render(data) shouldBe "X"
    }

    @Test
    fun `json empty string is truthy`() {
        val template = "{{#x}}X{{/x}}{{^x}}not X{{/x}}"
        val data = """{ "x": "" }"""
        Mustache(template).render(data) shouldBe "X"
    }

    @Test
    fun `json number`() {
        val template = "{{x}}"
        val data = """{ "x": 42 }"""
        Mustache(template).render(data) shouldBe "42"
    }

    @Test
    fun `json null`() {
        val template = "{{x}}"
        val data = """{ "x": null }"""
        Mustache(template).render(data) shouldBe ""
    }

    @Test
    fun `json true-false`() {
        val template = "{{x}}-{{y}}"
        val data = """{ "x": true, "y": false }"""
        Mustache(template).render(data) shouldBe "true-false"
    }
}
