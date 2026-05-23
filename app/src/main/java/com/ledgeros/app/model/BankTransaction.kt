package com.ledgeros.app.model

import java.time.LocalDate

data class BankTransaction(
    val id: String,
    val businessId: String,
    val transactionDate: LocalDate,
    val description: String,
    val amount: Double,
    val category: String,
    val gstEstimate: Double,
    val sourceFile: String,
    val matchedReceiptId: String? = null,
) {
    val isExpense: Boolean
        get() = amount < 0
}
