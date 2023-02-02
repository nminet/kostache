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

package dev.noemi.kostache.expects

import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.posix.arc4random_uniform


internal actual fun createTmpDir(): String {
    val path = "$tmpRoot/${arc4random_uniform(UInt.MAX_VALUE)}"
    val done = fileManager.createDirectoryAtPath(path, false, null, null)
    check(done)
    return path
}

internal actual fun writeFile(path: String, data: ByteArray) {
    fileManager.removeItemAtPath(path, null)
    val done = memScoped {
        val nsdata = NSData.create(bytes = allocArrayOf(data), length = data.size.toULong())
        fileManager.createFileAtPath(path, nsdata, null)
    }
    check(done)
}

internal actual fun deleteDir(path: String) {
    val done = fileManager.removeItemAtPath(path, null)
    check(done)
}

private val fileManager: NSFileManager by lazy {
    NSFileManager()
}

private val tmpRoot: String by lazy {
    NSTemporaryDirectory()
}
