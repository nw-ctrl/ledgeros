package com.ledgeros.app.service

import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.ui.state.CategoryBreakdownUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LedgerReviewAssistantTest {
    @Test
    fun buildsReviewBriefFromOpenIssues() {
        val brief = LedgerReviewAssistant().buildBrief(
            transactionsNeedingReview = listOf(
                BankTransaction(
                    id = "bank-1",
                    businessId = "business-demo",
                    transactionDate = LocalDate.of(2026, 2, 1),
                    description = "Unknown debit",
                    amount = -88.0,
                    category = "Uncategorised",
                    gstEstimate = 0.0,
                    sourceFile = "statement.csv",
                ),
            ),
            unmatchedExpenseCount = 2,
            duplicateReceipts = emptyList(),
            receiptMatchSuggestions = emptyList(),
            categoryBreakdown = listOf(
                CategoryBreakdownUiState(
                    category = "Software",
                    amount = 300.0,
                    gstEstimate = 27.27,
                    itemCount = 3,
                ),
            ),
            netGstPosition = -42.0,
        )

        assertEquals(28, brief.riskScore)
        assertTrue(brief.summary.contains("estimated credit"))
        assertTrue(brief.actions.any { it.contains("missing GST or category") })
    }
}
