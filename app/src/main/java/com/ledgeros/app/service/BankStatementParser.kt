package com.ledgeros.app.service

import com.ledgeros.app.model.BankTransaction
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

interface BankStatementParser {
    fun parse(statementName: String, content: String, businessId: String): List<BankTransaction>

    fun parse(statementName: String, content: ByteArray, businessId: String): List<BankTransaction> {
        return parse(statementName, content.toString(Charsets.UTF_8), businessId)
    }
}

class CsvBankStatementParser : BankStatementParser {
    override fun parse(statementName: String, content: String, businessId: String): List<BankTransaction> {
        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.size < 2) return emptyList()

        val delimiter = if (lines.first().count { it == '\t' } > lines.first().count { it == ',' }) '\t' else ','
        val headers = parseSeparatedLine(lines.first(), delimiter).map { it.normalizedHeader() }
        return lines.drop(1).mapNotNull { line ->
            val cells = parseSeparatedLine(line, delimiter)
            val date = cell(cells, headers, "date", "transactiondate")?.let(::parseDate) ?: return@mapNotNull null
            val description = cell(cells, headers, "description", "details", "narrative")
                ?.takeIf { it.isNotBlank() }
                ?: "Bank transaction"
            val amount = cell(cells, headers, "amount")
                ?.toMoneyOrNull()
                ?: signedAmount(
                    debit = cell(cells, headers, "debit", "withdrawal")?.toMoneyOrNull(),
                    credit = cell(cells, headers, "credit", "deposit")?.toMoneyOrNull(),
                )
                ?: return@mapNotNull null
            val category = cell(cells, headers, "category")?.takeIf { it.isNotBlank() } ?: inferCategory(description, amount)
            val gst = cell(cells, headers, "gst", "tax", "gstestimate")?.toMoneyOrNull()
                ?: estimateGst(amount, category)

            BankTransaction(
                id = "bank-${date}-$description-$amount".hashCode().toString(16),
                businessId = businessId,
                transactionDate = date,
                description = description,
                amount = amount,
                category = category.ifBlank { "Uncategorised" },
                gstEstimate = gst,
                sourceFile = statementName,
            )
        }
    }

    private fun cell(cells: List<String>, headers: List<String>, vararg names: String): String? {
        val index = names.firstNotNullOfOrNull { name -> headers.indexOf(name).takeIf { it >= 0 } }
        return index?.let { cells.getOrNull(it) }
    }

    private fun signedAmount(debit: Double?, credit: Double?): Double? {
        return when {
            debit != null && debit != 0.0 -> -kotlin.math.abs(debit)
            credit != null && credit != 0.0 -> kotlin.math.abs(credit)
            else -> null
        }
    }

    private fun parseDate(value: String): LocalDate? {
        value.toDoubleOrNull()
            ?.takeIf { it > 20_000.0 }
            ?.let { return LocalDate.of(1899, 12, 30).plusDays(it.toLong()) }
        return listOf(
            { LocalDate.parse(value) },
            { parseSlashDate(value) },
        ).firstNotNullOfOrNull { parser -> runCatching { parser() }.getOrNull() }
    }

    private fun parseSlashDate(value: String): LocalDate? {
        val parts = value.split("/", "-")
        if (parts.size != 3) return null
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val rawYear = parts[2].toInt()
        val year = if (rawYear < 100) 2000 + rawYear else rawYear
        return LocalDate.of(year, month, day)
    }

    internal fun parseSeparatedLine(line: String, delimiter: Char = ','): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        line.forEach { char ->
            when {
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> {
                    cells += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        cells += current.toString().trim()
        return cells
    }

    private fun String.toMoneyOrNull(): Double? {
        return replace("$", "")
            .replace(",", "")
            .trim()
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
    }

    private fun inferCategory(description: String, amount: Double): String {
        val text = description.lowercase()
        return when {
            amount > 0 -> "Sales"
            "fuel" in text || "uber" in text || "parking" in text -> "Motor vehicle"
            "software" in text || "saas" in text || "subscription" in text -> "Software"
            "office" in text || "paper" in text || "stationery" in text -> "Office supplies"
            else -> "Uncategorised"
        }
    }

    private fun estimateGst(amount: Double, category: String): Double {
        if (category == "Uncategorised") return 0.0
        return kotlin.math.abs(amount) / 11.0
    }
}

class AutoBankStatementParser(
    private val csvParser: CsvBankStatementParser = CsvBankStatementParser(),
    private val xlsxParser: XlsxBankStatementParser = XlsxBankStatementParser(csvParser),
) : BankStatementParser {
    override fun parse(statementName: String, content: String, businessId: String): List<BankTransaction> {
        return csvParser.parse(statementName, content, businessId)
    }

    override fun parse(statementName: String, content: ByteArray, businessId: String): List<BankTransaction> {
        return if (content.isZipPayload() || statementName.endsWith(".xlsx", ignoreCase = true)) {
            xlsxParser.parse(statementName, content, businessId)
        } else {
            csvParser.parse(statementName, content.toString(Charsets.UTF_8), businessId)
        }
    }
}

class XlsxBankStatementParser(
    private val csvParser: CsvBankStatementParser = CsvBankStatementParser(),
) {
    fun parse(statementName: String, content: ByteArray, businessId: String): List<BankTransaction> {
        val entries = unzipEntries(content)
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"].orEmpty())
        val sheetEntry = entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sorted()
            .firstOrNull()
            ?: return emptyList()
        val rows = parseSheetRows(entries[sheetEntry].orEmpty(), sharedStrings)
        if (rows.isEmpty()) return emptyList()
        val separated = rows.joinToString("\n") { row ->
            row.joinToString(",") { cell -> cell.csvEscaped() }
        }
        return csvParser.parse(statementName, separated, businessId)
    }

    private fun unzipEntries(content: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(content.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml")) {
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun parseSharedStrings(xml: String): List<String> {
        if (xml.isBlank()) return emptyList()
        val document = xml.toDocument()
        val items = document.getElementsByTagName("si")
        return (0 until items.length).map { index ->
            val textNodes = items.item(index).childNodes
            buildString {
                for (childIndex in 0 until textNodes.length) {
                    val child = textNodes.item(childIndex)
                    if (child.nodeName == "t") {
                        append(child.textContent)
                    } else {
                        val nestedText = child.childNodes
                        for (nestedIndex in 0 until nestedText.length) {
                            if (nestedText.item(nestedIndex).nodeName == "t") {
                                append(nestedText.item(nestedIndex).textContent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseSheetRows(xml: String, sharedStrings: List<String>): List<List<String>> {
        if (xml.isBlank()) return emptyList()
        val document = xml.toDocument()
        val rowNodes = document.getElementsByTagName("row")
        return (0 until rowNodes.length).map { rowIndex ->
            val cells = rowNodes.item(rowIndex).childNodes
            val valuesByColumn = mutableMapOf<Int, String>()
            for (cellIndex in 0 until cells.length) {
                val cell = cells.item(cellIndex)
                if (cell.nodeName != "c") continue
                val reference = cell.attributes?.getNamedItem("r")?.nodeValue.orEmpty()
                val columnIndex = reference.takeWhile { it.isLetter() }.toColumnIndex()
                val type = cell.attributes?.getNamedItem("t")?.nodeValue
                val rawValue = cell.childNodes.firstNodeText("v")
                val inlineValue = cell.childNodes.firstNodeText("t")
                valuesByColumn[columnIndex] = when (type) {
                    "s" -> sharedStrings.getOrNull(rawValue.toIntOrNull() ?: -1).orEmpty()
                    "inlineStr" -> inlineValue
                    else -> rawValue
                }
            }
            (0..(valuesByColumn.keys.maxOrNull() ?: -1)).map { columnIndex ->
                valuesByColumn[columnIndex].orEmpty()
            }
        }.filter { row -> row.any { it.isNotBlank() } }
    }
}

private fun ByteArray.isZipPayload(): Boolean {
    return size >= 2 && this[0] == 'P'.code.toByte() && this[1] == 'K'.code.toByte()
}

private fun String.normalizedHeader(): String {
    return lowercase().filter { it.isLetterOrDigit() }
}

private fun String.toDocument() = DocumentBuilderFactory.newInstance()
    .apply {
        isNamespaceAware = false
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    .newDocumentBuilder()
    .parse(byteInputStream())

private fun org.w3c.dom.NodeList.firstNodeText(name: String): String {
    for (index in 0 until length) {
        val node = item(index)
        if (node.nodeName == name) return node.textContent.orEmpty()
        val nested = node.childNodes.firstNodeText(name)
        if (nested.isNotBlank()) return nested
    }
    return ""
}

private fun String.toColumnIndex(): Int {
    if (isBlank()) return 0
    return fold(0) { total, char -> total * 26 + (char.uppercaseChar() - 'A' + 1) } - 1
}

private fun String.csvEscaped(): String {
    val escaped = replace("\"", "\"\"")
    return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
        "\"$escaped\""
    } else {
        escaped
    }
}
