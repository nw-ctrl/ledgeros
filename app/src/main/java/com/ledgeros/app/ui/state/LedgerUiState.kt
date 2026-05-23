package com.ledgeros.app.ui.state

import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.Business
import com.ledgeros.app.model.ComplianceTask
import com.ledgeros.app.model.GrantedTier
import com.ledgeros.app.model.ManagedUser
import com.ledgeros.app.model.Receipt

data class DashboardUiState(
    val business: Business,
    val recentReceipts: List<Receipt>,
    val complianceTasks: List<ComplianceTask>,
    val totalSpend: Double,
)

data class ComplianceUiState(
    val tasks: List<ComplianceTask>,
)

data class ReportsUiState(
    val selectedFinancialYearStart: Int,
    val currentFinancialYearStart: Int,
    val financialYearLabel: String,
    val financialYearDateRange: String,
    val isCurrentFinancialYear: Boolean,
    val gstOnPurchases: Double,
    val totalTrackedExpenses: Double,
    val gstOnSales: Double,
    val totalTrackedSales: Double,
    val netGstPosition: Double,
    val evidenceCoveragePercent: Int,
    val reportHealthSummary: String,
    val reconciliationSummary: String,
    val matchedTransactionCount: Int,
    val unmatchedExpenseCount: Int,
    val aiRiskScore: Int,
    val aiReviewBrief: String,
    val aiReviewActions: List<String>,
    val exportFileName: String,
    val exportCsv: String,
    val exportSummary: String,
    val bankStatementStatus: String,
    val importedStatementCount: Int,
    val backlogTransactionCount: Int,
    val transactionsNeedingReview: List<BankTransaction>,
    val smartInsights: List<String>,
    val categoryBreakdown: List<CategoryBreakdownUiState>,
    val duplicateReceiptCandidates: List<DuplicateReceiptCandidateUiState>,
    val receiptMatchSuggestions: List<ReceiptMatchSuggestionUiState>,
    val basPeriods: List<BasPeriodUiState>,
    val bankTransactions: List<BankTransaction>,
)

data class CategoryBreakdownUiState(
    val category: String,
    val amount: Double,
    val gstEstimate: Double,
    val itemCount: Int,
)

data class DuplicateReceiptCandidateUiState(
    val receiptIds: List<String>,
    val merchant: String,
    val date: String,
    val total: Double,
    val count: Int,
)

data class ReceiptMatchSuggestionUiState(
    val transactionId: String,
    val transactionDescription: String,
    val receiptId: String,
    val receiptMerchant: String,
    val confidencePercent: Int,
    val amountDifference: Double,
    val dayDifference: Long,
)

data class BasPeriodUiState(
    val id: String,
    val label: String,
    val dateRange: String,
    val startDate: String,
    val endDate: String,
    val dueDate: String,
    val receiptCount: Int,
    val bankTransactionCount: Int,
    val sales: Double,
    val expenses: Double,
    val gstOnSales: Double,
    val gstOnPurchases: Double,
    val reviewFlagCount: Int,
    val transactionIds: List<String>,
    val receiptIds: List<String>,
) {
    val netGst: Double
        get() = gstOnSales - gstOnPurchases
}

data class ReceiptUiState(
    val selectedImageUri: String?,
    val sampleText: String,
    val status: String,
    val reviewDraft: ReceiptReviewDraftUiState,
    val deterministicFirst: Boolean,
    val fallbackOcrProvider: Boolean,
    val innovationMode: Boolean,
    val isOcrRunning: Boolean = false,
    val receiptSaved: Boolean = false,
)

data class ReceiptReviewDraftUiState(
    val merchant: String = "",
    val receiptDate: String = "",
    val total: String = "",
    val gst: String = "",
    val category: String = "",
    val confidence: String = "",
) {
    val hasContent: Boolean
        get() = listOf(merchant, receiptDate, total, gst, category).any { it.isNotBlank() }
}

data class SettingsUiState(
    val deterministicFirst: Boolean,
    val maskSensitiveIdentifiers: Boolean,
    val fallbackOcrProvider: Boolean,
    val innovationMode: Boolean,
)

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** True after signUp() succeeds when Supabase email confirmation is enabled. */
    val awaitingEmailConfirmation: Boolean = false,
    val confirmationEmail: String = "",
    /** Non-null when the owner has explicitly granted this user a tier. */
    val grantedTier: GrantedTier? = null,
)

data class AdminUiState(
    val managedUsers: List<ManagedUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class LedgerUiState(
    val auth: AuthUiState = AuthUiState(),
    val dashboard: DashboardUiState,
    val compliance: ComplianceUiState,
    val reports: ReportsUiState,
    val receipts: ReceiptUiState,
    val settings: SettingsUiState,
    val admin: AdminUiState = AdminUiState(),
    val isOnboardingComplete: Boolean = false,
    val isPremium: Boolean = false,
    /** True only for the owner account (ns@nextwave.au). */
    val isOwner: Boolean = false,
)

// Premium tier constants
object PremiumTier {
    const val FREE_RECEIPT_LIMIT = 10
    const val PRO_MONTHLY_SKU = "ledgeros_pro_monthly"
    const val PRO_YEARLY_SKU = "ledgeros_pro_yearly"
    const val PRO_MONTHLY_PRICE = "A$7.99"
    const val PRO_YEARLY_PRICE = "A$59.99"
    const val PRO_YEARLY_SAVING = "Save 37%"
}
