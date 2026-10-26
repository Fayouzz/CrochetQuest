package com.crochetquest.app.domain.backup

/**
 * A tiny, dependency-free JSON reader/writer, so backups can be unit-tested on a plain JVM.
 * Values are Map<String, Any?>, List<Any?>, String, Long, Double, Boolean or null.
 */
object MiniJson {
    private const val MAX_DEPTH = 40

    fun write(value: Any?): String = StringBuilder().also { write(value, it, 0) }.toString()

    private fun write(value: Any?, out: StringBuilder, depth: Int) {
        when (value) {
            null -> out.append("null")
            is Boolean -> out.append(value.toString())
            is Int, is Long -> out.append(value.toString())
            is Double -> out.append(if (value.isNaN() || value.isInfinite()) "null" else value.toString())
            is String -> writeString(value, out)
            is Map<*, *> -> {
                if (value.isEmpty()) { out.append("{}"); return }
                out.append("{\n")
                var first = true
                for ((k, v) in value) {
                    if (!first) out.append(",\n")
                    first = false
                    indent(out, depth + 1)
                    writeString(k.toString(), out)
                    out.append(": ")
                    write(v, out, depth + 1)
                }
                out.append('\n')
                indent(out, depth)
                out.append('}')
            }
            is List<*> -> {
                if (value.isEmpty()) { out.append("[]"); return }
                // Rows of plain values stay on one line so a backup file stays readable.
                val flat = value.all { it !is Map<*, *> && it !is List<*> }
                if (flat) {
                    out.append('[')
                    value.forEachIndexed { i, v ->
                        if (i > 0) out.append(", ")
                        write(v, out, depth + 1)
                    }
                    out.append(']')
                } else {
                    out.append("[\n")
                    value.forEachIndexed { i, v ->
                        if (i > 0) out.append(",\n")
                        indent(out, depth + 1)
                        write(v, out, depth + 1)
                    }
                    out.append('\n')
                    indent(out, depth)
                    out.append(']')
                }
            }
            else -> writeString(value.toString(), out)
        }
    }

    private fun indent(out: StringBuilder, depth: Int) {
        repeat(depth) { out.append("  ") }
    }

    private fun writeString(s: String, out: StringBuilder) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c.code < 0x20 -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    /** Parses [text] or throws [BackupFormatException]. */
    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.readValue(0)
        parser.skipWhitespace()
        if (!parser.atEnd()) throw BackupFormatException("The file has extra content after the data.")
        return value
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun atEnd() = i >= s.length

        fun skipWhitespace() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        private fun fail(what: String): Nothing =
            throw BackupFormatException("This doesn't look like a valid backup ($what).")

        fun readValue(depth: Int): Any? {
            if (depth > MAX_DEPTH) fail("too deeply nested")
            skipWhitespace()
            if (atEnd()) fail("unexpected end of file")
            return when (val c = s[i]) {
                '{' -> readObject(depth)
                '[' -> readArray(depth)
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) readNumber() else fail("unexpected '$c'")
            }
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!s.startsWith(word, i)) fail("unexpected text")
            i += word.length
            return value
        }

        private fun readObject(depth: Int): Map<String, Any?> {
            i++ // {
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (i < s.length && s[i] == '}') { i++; return map }
            while (true) {
                skipWhitespace()
                if (atEnd() || s[i] != '"') fail("expected a name")
                val key = readString()
                skipWhitespace()
                if (atEnd() || s[i] != ':') fail("expected ':'")
                i++
                map[key] = readValue(depth + 1)
                skipWhitespace()
                if (atEnd()) fail("unexpected end of file")
                when (s[i]) {
                    ',' -> i++
                    '}' -> { i++; return map }
                    else -> fail("expected ',' or '}'")
                }
            }
        }

        private fun readArray(depth: Int): List<Any?> {
            i++ // [
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (i < s.length && s[i] == ']') { i++; return list }
            while (true) {
                list += readValue(depth + 1)
                skipWhitespace()
                if (atEnd()) fail("unexpected end of file")
                when (s[i]) {
                    ',' -> i++
                    ']' -> { i++; return list }
                    else -> fail("expected ',' or ']'")
                }
            }
        }

        private fun readString(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) fail("unterminated text")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (atEnd()) fail("unterminated text")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (i + 4 > s.length) fail("bad escape")
                                val code = s.substring(i, i + 4).toIntOrNull(16) ?: fail("bad escape")
                                sb.append(code.toChar())
                                i += 4
                            }
                            else -> fail("bad escape '$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readNumber(): Any {
            val start = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' ||
                    s[i] == '+' || s[i] == '-')) i++
            val token = s.substring(start, i)
            return token.toLongOrNull() ?: token.toDoubleOrNull() ?: fail("bad number '$token'")
        }
    }
}
