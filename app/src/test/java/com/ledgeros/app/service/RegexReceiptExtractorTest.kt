package com.ledgeros.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RegexReceiptExtractorTest {
    @Test
    fun extractsAustralianReceiptFields() {
        val result = RegexReceiptExtractor().extract(
            "Officeworks ABN 36 004 763 526 Receipt OW-2048 Date 12/05/2026 GST $3.86 Total $42.50",
        )

        assertEquals("36004763526", result.abn)
        assertEquals("OW-2048", result.invoiceNumber)
        assertEquals(3.86, result.gst ?: 0.0, 0.001)
        assertEquals(42.50, result.total ?: 0.0, 0.001)
        assertNotNull(result.receiptDate)
    }
}
