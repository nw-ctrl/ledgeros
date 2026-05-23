package com.ledgeros.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ── Request / Response DTOs ───────────────────────────────────────────────
// Field names use @SerializedName to match the FastAPI snake_case Pydantic
// models exactly. Keep these in sync with backend/python/app/main.py.

data class TransactionDto(
    @SerializedName("transaction_date") val transactionDate: String,   // ISO-8601 date string
    val description: String,
    val amount: Double,
    val category: String = "Uncategorised",
    @SerializedName("gst_estimate") val gstEstimate: Double = 0.0,
    val source: String = "bank",                                        // "bank" | "receipt" | "manual"
)

// ── /bas/prepare ─────────────────────────────────────────────────────────

data class BasPreparationRequest(
    @SerializedName("business_id") val businessId: String,
    @SerializedName("financial_year") val financialYear: String = "2025-26",
    val transactions: List<TransactionDto>,
)

data class BasQuarterDto(
    val label: String,
    @SerializedName("date_range") val dateRange: String,
    val sales: Double,
    val expenses: Double,
    @SerializedName("gst_on_sales") val gstOnSales: Double,
    @SerializedName("gst_on_purchases") val gstOnPurchases: Double,
    @SerializedName("net_gst_estimate") val netGstEstimate: Double,
    @SerializedName("review_flags") val reviewFlags: List<String> = emptyList(),
)

data class BasPreparationResponse(
    @SerializedName("business_id") val businessId: String,
    @SerializedName("financial_year") val financialYear: String,
    val quarters: List<BasQuarterDto>,
    val disclaimer: String,
)

// ── /ai/review-brief ──────────────────────────────────────────────────────

data class AiReviewRequest(
    @SerializedName("business_id") val businessId: String,
    @SerializedName("financial_year") val financialYear: String = "2025-26",
    val transactions: List<TransactionDto>,
    @SerializedName("unmatched_expense_count") val unmatchedExpenseCount: Int = 0,
    @SerializedName("duplicate_receipt_count") val duplicateReceiptCount: Int = 0,
    @SerializedName("suggested_match_count") val suggestedMatchCount: Int = 0,
)

data class AiReviewResponse(
    @SerializedName("risk_score") val riskScore: Int,
    val summary: String,
    val actions: List<String>,
    @SerializedName("audit_note") val auditNote: String,
)

// ── Retrofit interface ────────────────────────────────────────────────────

interface LedgerApiService {
    /** Ping — returns {"status": "ok"} */
    @GET("health")
    suspend fun health(): Map<String, String>

    /**
     * Server-side BAS preparation: splits transactions into ATO quarters,
     * computes GST totals, and flags uncategorised items for review.
     */
    @POST("bas/prepare")
    suspend fun prepareBas(@Body request: BasPreparationRequest): BasPreparationResponse

    /**
     * AI review brief: returns a risk score (0-100), a one-sentence summary,
     * and up to 4 prioritised cleanup actions.
     *
     * The Android app shows the locally-computed version instantly, then
     * silently upgrades it with this server response when it arrives.
     */
    @POST("ai/review-brief")
    suspend fun aiReviewBrief(@Body request: AiReviewRequest): AiReviewResponse
}
