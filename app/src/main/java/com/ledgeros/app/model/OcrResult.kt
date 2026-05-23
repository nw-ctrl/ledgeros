package com.ledgeros.app.model

import java.time.LocalDate

data class OcrResult(
    val rawTextHash: String,
    val abn: String?,
    val gst: Double?,
    val total: Double?,
    val invoiceNumber: String?,
    val receiptDate: LocalDate?,
    val confidence: Double,
)
