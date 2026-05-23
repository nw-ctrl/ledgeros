package com.ledgeros.app.service

import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.ui.state.CategoryBreakdownUiState
import com.ledgeros.app.ui.state.DuplicateReceiptCandidateUiState
import com.ledgeros.app.ui.state.ReceiptMatchSuggestionUiState
import java.util.Locale
import kotlin.math.abs

data class LedgerReviewBrief(
    val riskScore: Int,
    val summary: String,
    val actions: List<String>,
)

class LedgerReviewAssistant {
    fun buildBrief(
        transactionsNeedingReview: List<BankTransaction>,
        unmatchedExpenseCount: Int,
        duplicateReceipts: List<DuplicateReceiptCandidateUiState>,
        receiptMatchSuggestions: List<ReceiptMatchSuggestionUiState>,
        categoryBreakdown: List<CategoryBreakdownUiState>,
        netGstPosition: Double,
    ): LedgerReviewBrief {
        val duplicateCount = duplicateReceipts.sumOf { it.count - 1 }
        val riskScore = (
            transactionsNeedingReview.size * 12 +
                unmatchedExpenseCount * 8 +
                duplicateCount * 10 +
                receiptMatchSuggestions.size * 4
            ).coerceIn(0, 100)
        val largestCategory = categoryBreakdown.firstOrNull()
        val actions = buildList {
            if (transactionsNeedingReview.isNotEmpty()) {
                add("Review ${transactionsNeedingReview.size} transactions with missing GST or category.")
            }
            if (receiptMatchSuggestions.isNotEmpty()) {
                add("Accept or clear ${receiptMatchSuggestions.size} suggested receipt matches.")
            }
            if (unmatchedExpenseCount > 0) {
                add("Attach evidence for $unmatchedExpenseCount unmatched expense transactions.")
            }
            if (duplicateCount > 0) {
                add("Check $duplicateCount possible duplicate receipt entries before export.")
            }
            largestCategory?.let {
                add("Spot-check ${it.category}; it is the largest spend area at ${it.amount.moneyLabel()}.")
            }
        }.ifEmpty {
            listOf("No urgent cleanup detected. Export is ready for human review.")
        }.take(4)

        val posture = when {
            riskScore >= 70 -> "High review load"
            riskScore >= 35 -> "Moderate review load"
            riskScore > 0 -> "Light review load"
            else -> "Clean preparation set"
        }
        val netPosition = if (netGstPosition >= 0.0) {
            "${netGstPosition.moneyLabel()} estimated payable"
        } else {
            "${abs(netGstPosition).moneyLabel()} estimated credit"
        }

        return LedgerReviewBrief(
            riskScore = riskScore,
            summary = "$posture. Current BAS workspace shows $netPosition before final review.",
            actions = actions,
        )
    }

    private fun Double.moneyLabel(): String {
        return "$" + String.format(Locale.US, "%,.2f", this)
    }
}
