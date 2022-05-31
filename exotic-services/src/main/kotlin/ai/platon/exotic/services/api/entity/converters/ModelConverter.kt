package ai.platon.exotic.services.api.entity.converters

import org.apache.commons.lang3.StringUtils

open class ModelConverter {
    /**
     * aGoodName -> a_good_name
     */
    open fun underlineStyle(text: String): String? {
        var text0 = text.trim { it <= ' ' || it == '_' }
        text0 = StringUtils.splitByCharacterTypeCamelCase(text0)
            .onEach { it.trim { it <= ' ' || it == '_' } }
            .filter { StringUtils.isAlphanumeric(it) }
            .joinToString("_")
        return text0.lowercase()
    }

    fun s(props: Map<String, Any?>, name: String): String? {
        return props[underlineStyle(name)]?.toString()
    }

    fun s(f: Any?): String? {
        return f?.toString()
    }

    fun i(f: Any?, defaultValue: Int = 0): Int {
        return f?.toString()?.toIntOrNull() ?: defaultValue
    }

    fun i(props: Map<String, Any?>, name: String, defaultValue: Int = 0): Int {
        return i(props[underlineStyle(name)]?.toString(), defaultValue)
    }

    fun l(f: Any?, defaultValue: Long = 0): Long {
        return f?.toString()?.toLongOrNull() ?: defaultValue
    }

    fun l(props: Map<String, Any?>, name: String, defaultValue: Long = 0): Long {
        return l(props[underlineStyle(name)]?.toString(), defaultValue)
    }

    fun f(f: Any?, defaultValue: Float = -1.0f): Float {
        return f?.toString()?.toFloatOrNull() ?: defaultValue
    }

    fun f(props: Map<String, Any?>, name: String, defaultValue: Float = 0.0f): Float {
        return f(props[underlineStyle(name)]?.toString(), defaultValue)
    }

    fun d(f: Any?, defaultValue: Double = -1.0): Double {
        return f?.toString()?.toDoubleOrNull() ?: defaultValue
    }

    fun d(props: Map<String, Any?>, name: String, defaultValue: Double = 0.0): Double {
        return d(props[underlineStyle(name)]?.toString(), defaultValue)
    }
}
