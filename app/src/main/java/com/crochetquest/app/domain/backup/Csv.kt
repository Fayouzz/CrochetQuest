package com.crochetquest.app.domain.backup

/** One CSV cell. [quoted] lets us tell an empty text ("") apart from a missing value (nothing). */
data class CsvField(val text: String, val quoted: Boolean)

/** A small RFC 4180 style CSV reader/writer (quotes, commas and line breaks inside cells). */
object Csv {

    /** Cells that are null are written as nothing; empty text is written as "". */
    fun writeRecord(values: List<Any?>): String = values.joinToString(",") { writeCell(it) }

    private fun writeCell(value: Any?): String {
        if (value == null) return ""
        val text = value.toString()
        val needsQuotes = text.isEmpty() || text.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
            text.first() == ' ' || text.last() == ' '
        return if (needsQuotes) "\"" + text.replace("\"", "\"\"") + "\"" else text
    }

    /** Splits the whole text into records of cells. A leading byte-order mark is ignored. */
    fun parse(input: String): List<List<CsvField>> {
        val text = input.removePrefix("\uFEFF")
        val records = ArrayList<List<CsvField>>()
        var fields = ArrayList<CsvField>()
        val cell = StringBuilder()
        var quoted = false
        var inQuotes = false
        var anyContent = false
        var i = 0

        fun endField() {
            fields.add(CsvField(cell.toString(), quoted))
            cell.clear()
            quoted = false
        }

        fun endRecord() {
            endField()
            records.add(fields)
            fields = ArrayList()
            anyContent = false
        }

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        if (cell.isNotEmpty()) throw BackupFormatException("The CSV file has a misplaced quote.")
                        inQuotes = true; quoted = true; anyContent = true
                    }
                    ',' -> { endField(); anyContent = true }
                    '\r' -> { /* handled with the following \n */ }
                    '\n' -> endRecord()
                    else -> { cell.append(c); anyContent = true }
                }
            }
            i++
        }
        if (inQuotes) throw BackupFormatException("The CSV file ends inside a quoted value.")
        if (anyContent || cell.isNotEmpty() || fields.isNotEmpty()) endRecord()
        return records
    }
}
