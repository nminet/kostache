package dev.noemi.kostache

import java.io.File
import java.io.IOException

internal actual fun readText(dirname: String, basename: String): String? {
    return try {
        File(dirname, basename).readText()
    } catch (e: IOException) {
        null
    }
}
