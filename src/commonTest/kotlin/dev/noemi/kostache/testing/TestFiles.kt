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

package dev.noemi.kostache.testing

import dev.noemi.kostache.testing.expects.createTmpDir
import dev.noemi.kostache.testing.expects.deleteDir

class TestFiles {

    fun writeFile(basename: String, data: ByteArray): TestFiles {
        dev.noemi.kostache.testing.expects.writeFile("$dirname/$basename", data)
        return this
    }

    fun writeFile(basename: String, text: String): TestFiles {
        writeFile(basename, text.encodeToByteArray())
        return this
    }

    fun <R> use(block: (TestFiles) -> R): R {
        return block(this).also {
            if (dirname.isNotEmpty()) {
                deleteDir(dirname)
            }
        }
    }

    val dirname: String by lazy {
        createTmpDir()
    }
}
