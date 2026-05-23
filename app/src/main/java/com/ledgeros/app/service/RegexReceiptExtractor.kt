package com.ledgeros.app.service

import com.ledgeros.app.model.OcrResult
import java.time.LocalDate

class RegexReceiptExtractor {
    fun extract(text: String): OcrResult {
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        val abn = Regex("\\bABN[:\\s]*([0-9 ]{11,14})\\b", RegexOption.IGNORE_CASE)
            .find(normalized)
            ?.groupValues
            ?.get(1)
            ?.replace(" ", "")
        val invoiceNumber = Regex(
            "\\b(?:invoice|inv|receipt)[:#\\s-]*([A-Z0-9-]{4,})\\b",
            RegexOption.IGNORE_CASE,
        ).find(normalized)?.groupValues?.get(1)
        val gst = moneyAfterLabel(normalized, "GST")
        val total = moneyAfterLabel(normalized, "TOTAL") ?: moneyAfterLabel(normalized, "AMOUNT")
        val receiptDate = date(normalized)
        val confidence = listOf(abn, invoiceNumber, gst, total, receiptDate)
            .count { it != null }
            .toDouble() / 5.0

        return OcrResult(
            rawTextHash = normalized.hashCode().toString(16),
            abn = abn,
            gst = gst,
            total = total,
            invoiceNumber = invoiceNumber,
            receiptDate = receiptDate,
            confidence = confidence,
        )
    }

    private fun moneyAfterLabel(text: String, label: String): Double? {
        val match = Regex(
            "$label[^0-9]{0,12}\\$?([0-9]+(?:\\.[0-9]{2})?)",
            RegexOption.IGNORE_CASE,
        ).find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun date(text: String): LocalDate? {
        val match = Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b")
            .find(text) ?: return null
        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val rawYear = match.groupValues[3].toInt()
        val year = if (rawYear < 100) 2000 + rawYear else rawYear
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }
}
