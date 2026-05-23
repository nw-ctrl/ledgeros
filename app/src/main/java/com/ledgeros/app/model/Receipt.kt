package com.ledgeros.app.model

import java.time.LocalDate

data class Receipt(
    val id: String,
    val businessId: String,
    val fileUrl: String,
    val merchant: String,
    val receiptDate: LocalDate,
    val total: Double,
    val gst: Double,
    val category: String,
    val ocrConfidence: Double,
    val aiConfidence: Double,
)
