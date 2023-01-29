package dev.noemi.kostache


internal expect fun property(obj: Any, name: String, body: String?): Any?

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

    companion object {
        val wrap = { data: Any? ->
            KClassContext(data)
        }
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
