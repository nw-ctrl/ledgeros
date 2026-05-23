package com.ledgeros.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.ledgeros.app.data.DemoLedgerRepository
import com.ledgeros.app.data.LedgerRepository
import com.ledgeros.app.data.RoomLedgerRepository
import com.ledgeros.app.data.SettingsDataStore
import com.ledgeros.app.model.Receipt
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.service.AiCategorizationPayload
import com.ledgeros.app.service.AiCategorizationService
import com.ledgeros.app.service.AutoBankStatementParser
import com.ledgeros.app.service.BankStatementParser
import com.ledgeros.app.service.DemoLocalReceiptOcrService
import com.ledgeros.app.service.LedgerReviewAssistant
import com.ledgeros.app.service.LocalReceiptOcrService
import com.ledgeros.app.service.MlKitReceiptOcrService
import com.ledgeros.app.service.RegexReceiptExtractor
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.ui.state.BasPeriodUiState
import com.ledgeros.app.ui.state.CategoryBreakdownUiState
import com.ledgeros.app.ui.state.ComplianceUiState
import com.ledgeros.app.ui.state.DashboardUiState
import com.ledgeros.app.ui.state.DuplicateReceiptCandidateUiState
import com.ledgeros.app.ui.state.LedgerUiState
import com.ledgeros.app.ui.state.ReceiptReviewDraftUiState
import com.ledgeros.app.ui.state.ReceiptMatchSuggestionUiState
import com.ledgeros.app.ui.state.ReceiptUiState
import com.ledgeros.app.ui.state.ReportsUiState
import com.ledgeros.app.ui.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val settingsDataStore: SettingsDataStore? = null,
    private val extractor: RegexReceiptExtractor = RegexReceiptExtractor(),
    private val aiCategorizationService: AiCategorizationService = AiCategorizationService(),
    private val ledgerReviewAssistant: LedgerReviewAssistant = LedgerReviewAssistant(),
    private val localOcrService: LocalReceiptOcrService = DemoLocalReceiptOcrService(),
    private val bankStatementParser: BankStatementParser = AutoBankStatementParser(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        settingsDataStore?.let { ds ->
            viewModelScope.launch {
                ds.settingsFlow.collect { saved ->
                    _uiState.update { state ->
                        state.copy(
                            settings = saved,
                            receipts = state.receipts.copy(
                                deterministicFirst = saved.deterministicFirst,
                                fallbackOcrProvider = saved.fallbackOcrProvider,
                                innovationMode = saved.innovationMode,
                            ),
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            val snapshot = repository.loadSnapshot()
            _uiState.update { state ->
                state.copy(
                    dashboard = state.dashboard.copy(
                        business = snapshot.activeBusiness,
                        recentReceipts = snapshot.recentReceipts,
                        complianceTasks = snapshot.complianceTasks,
                        totalSpend = snapshot.recentReceipts.sumOf { it.total },
                    ),
                    compliance = ComplianceUiState(tasks = snapshot.complianceTasks),
                    reports = buildReportsState(
                        receipts = snapshot.recentReceipts,
                        bankTransactions = snapshot.bankTransactions,
                        financialYearStart = state.reports.selectedFinancialYearStart,
                    ),
                )
            }
        }
    }

    fun updateReceiptSample(text: String) {
        _uiState.update { state ->
            state.copy(receipts = state.receipts.copy(sampleText = text))
        }
    }

    fun runReceiptExtraction() {
        val state = _uiState.value
        val result = extractor.extract(state.receipts.sampleText)
        val merchant = state.receipts.sampleText.lineSequence()
            .firstOrNull()
            ?.substringBefore(" ABN")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown merchant"
        val category = aiCategorizationService.categorize(
            AiCategorizationPayload(
                merchant = merchant,
                amount = result.total ?: 0.0,
                description = state.receipts.sampleText.take(120),
            ),
        )
        val statusPrefix = if (state.receipts.innovationMode) {
            "Innovation mode scanned"
        } else {
            "Deterministic extraction scanned"
        }

        _uiState.update { current ->
            current.copy(
                receipts = current.receipts.copy(
                    status = "$statusPrefix ${result.abn ?: "no ABN"} / ${result.total ?: 0.0} as ${category.category}.",
                    reviewDraft = ReceiptReviewDraftUiState(
                        merchant = merchant,
                        receiptDate = result.receiptDate?.toString().orEmpty(),
                        total = result.total?.let { String.format(Locale.US, "%.2f", it) }.orEmpty(),
                        gst = result.gst?.let { String.format(Locale.US, "%.2f", it) }.orEmpty(),
                        category = category.category,
                        confidence = String.format(
                            Locale.US,
                            "%.0f%% OCR / %.0f%% AI",
                            result.confidence * 100,
                            category.confidence * 100,
                        ),
                    ),
                ),
            )
        }
    }

    fun selectReceiptImage(imageUri: String) {
        _uiState.update { state ->
            state.copy(
                receipts = state.receipts.copy(
                    selectedImageUri = imageUri,
                    isOcrRunning = true,
                    receiptSaved = false,
                    reviewDraft = ReceiptReviewDraftUiState(),
                    sampleText = "",
                    status = "Scanning receipt image…",
                ),
            )
        }
        viewModelScope.launch {
            val ocrText = try {
                localOcrService.extractText(imageUri)
            } catch (e: Exception) {
                ""
            }
            _uiState.update { state ->
                state.copy(
                    receipts = state.receipts.copy(
                        sampleText = ocrText.ifBlank { state.receipts.sampleText },
                        isOcrRunning = false,
                        status = if (ocrText.isNotBlank()) {
                            "OCR complete — ${ocrText.length} characters extracted. Review text then tap Extract."
                        } else {
                            "No text detected. Try a clearer photo or paste the receipt text manually."
                        },
                    ),
                )
            }
            if (ocrText.isNotBlank()) {
                runReceiptExtraction()
            }
        }
    }

    fun markReceiptUploadReady() {
        _uiState.update { state ->
            val status = if (state.receipts.fallbackOcrProvider) {
                "Choose an image. Local OCR will run first, with provider fallback enabled."
            } else {
                "Choose an image. Local OCR will prepare a review draft."
            }
            state.copy(receipts = state.receipts.copy(status = status))
        }
    }

    fun updateReceiptDraftMerchant(value: String) = updateReceiptDraft { it.copy(merchant = value) }

    fun updateReceiptDraftDate(value: String) = updateReceiptDraft { it.copy(receiptDate = value) }

    fun updateReceiptDraftTotal(value: String) = updateReceiptDraft { it.copy(total = value) }

    fun updateReceiptDraftGst(value: String) = updateReceiptDraft { it.copy(gst = value) }

    fun updateReceiptDraftCategory(value: String) = updateReceiptDraft { it.copy(category = value) }

    fun updateTaskStatus(taskId: String, status: ComplianceStatus) {
        _uiState.update { state ->
            val tasks = state.compliance.tasks.map {
                if (it.id == taskId) it.copy(status = status) else it
            }
            viewModelScope.launch { repository.updateComplianceTaskStatus(taskId, status) }
            state.copy(
                compliance = ComplianceUiState(tasks = tasks),
                dashboard = state.dashboard.copy(complianceTasks = tasks),
            )
        }
    }

    fun saveReceiptDraft() {
        _uiState.update { state ->
            val draft = state.receipts.reviewDraft
            val receipt = Receipt(
                id = "receipt-review-${System.currentTimeMillis()}",
                businessId = state.dashboard.business.id,
                fileUrl = state.receipts.selectedImageUri.orEmpty(),
                merchant = draft.merchant.ifBlank { "Unknown merchant" },
                receiptDate = runCatching { LocalDate.parse(draft.receiptDate) }.getOrDefault(LocalDate.now()),
                total = draft.total.toDoubleOrNull() ?: 0.0,
                gst = draft.gst.toDoubleOrNull() ?: 0.0,
                category = draft.category.ifBlank { "Uncategorised" },
                ocrConfidence = 0.80,
                aiConfidence = 0.72,
            )
            val receipts = listOf(receipt) + state.dashboard.recentReceipts
            viewModelScope.launch { repository.saveReceipt(receipt) }

            state.copy(
                dashboard = state.dashboard.copy(
                    recentReceipts = receipts,
                    totalSpend = receipts.sumOf { it.total },
                ),
                reports = buildReportsState(
                    receipts = receipts,
                    bankTransactions = state.reports.bankTransactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                )
                    .copy(
                        bankStatementStatus = state.reports.bankStatementStatus,
                        importedStatementCount = state.reports.importedStatementCount,
                    ),
                receipts = state.receipts.copy(
                    status = "Receipt saved. Dashboard and reports updated.",
                    selectedImageUri = null,
                    sampleText = "",
                    reviewDraft = ReceiptReviewDraftUiState(),
                    isOcrRunning = false,
                    receiptSaved = true,
                ),
            )
        }
    }

    fun importBankStatement(statementName: String, content: ByteArray) {
        _uiState.update { state ->
            val imported = bankStatementParser.parse(statementName, content, state.dashboard.business.id)
            val transactions = imported + state.reports.bankTransactions
            viewModelScope.launch { repository.saveBankTransactions(imported) }
            val status = if (imported.isEmpty()) {
                "No rows were imported. Check the file has date/amount or debit/credit columns."
            } else {
                "Imported ${imported.size} bank transactions from CSV or Excel into ${state.reports.financialYearLabel}."
            }
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = transactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                )
                    .copy(
                        bankStatementStatus = status,
                        importedStatementCount = state.reports.importedStatementCount + 1,
                    ),
            )
        }
    }

    fun addManualAdjustment(
        transactionDate: String,
        description: String,
        amount: String,
        category: String,
        gstEstimate: String,
    ) {
        _uiState.update { state ->
            val parsedDate = runCatching { LocalDate.parse(transactionDate) }.getOrNull()
            val parsedAmount = amount.toDoubleOrNull()
            if (parsedDate == null || parsedAmount == null) {
                return@update state.copy(
                    reports = state.reports.copy(
                        bankStatementStatus = "Manual adjustment needs a date like 2026-05-19 and a numeric amount.",
                    ),
                )
            }
            val transaction = BankTransaction(
                id = "manual-${System.currentTimeMillis()}",
                businessId = state.dashboard.business.id,
                transactionDate = parsedDate,
                description = description.ifBlank { "Manual BAS adjustment" },
                amount = parsedAmount,
                category = category.ifBlank { "Manual adjustment" },
                gstEstimate = gstEstimate.toDoubleOrNull() ?: 0.0,
                sourceFile = "Manual adjustment",
            )
            val transactions = listOf(transaction) + state.reports.bankTransactions
            viewModelScope.launch { repository.saveBankTransactions(listOf(transaction)) }
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = transactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = "Manual adjustment added. Open it from recent transactions to edit later.",
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    fun updateReceipt(
        receiptId: String,
        merchant: String,
        receiptDate: String,
        total: String,
        gst: String,
        category: String,
    ) {
        _uiState.update { state ->
            val existing = state.dashboard.recentReceipts.firstOrNull { it.id == receiptId } ?: return@update state
            val updated = existing.copy(
                merchant = merchant.ifBlank { existing.merchant },
                receiptDate = parseDateOrDefault(receiptDate, existing.receiptDate),
                total = total.toDoubleOrNull() ?: existing.total,
                gst = gst.toDoubleOrNull() ?: existing.gst,
                category = category.ifBlank { "Uncategorised" },
            )
            val receipts = state.dashboard.recentReceipts.map { if (it.id == receiptId) updated else it }
            viewModelScope.launch { repository.updateReceipt(updated) }
            state.copy(
                dashboard = state.dashboard.copy(
                    recentReceipts = receipts,
                    totalSpend = receipts.sumOf { it.total },
                ),
                reports = buildReportsState(
                    receipts = receipts,
                    bankTransactions = state.reports.bankTransactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = state.reports.bankStatementStatus,
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    fun updateBankTransaction(
        transactionId: String,
        description: String,
        transactionDate: String,
        amount: String,
        category: String,
        gstEstimate: String,
    ) {
        _uiState.update { state ->
            val existing = state.reports.bankTransactions.firstOrNull { it.id == transactionId } ?: return@update state
            val updated = existing.copy(
                description = description.ifBlank { existing.description },
                transactionDate = parseDateOrDefault(transactionDate, existing.transactionDate),
                amount = amount.toDoubleOrNull() ?: existing.amount,
                category = category.ifBlank { "Uncategorised" },
                gstEstimate = gstEstimate.toDoubleOrNull() ?: existing.gstEstimate,
            )
            val transactions = state.reports.bankTransactions.map { if (it.id == transactionId) updated else it }
            viewModelScope.launch { repository.updateBankTransaction(updated) }
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = transactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = state.reports.bankStatementStatus,
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    fun acceptReceiptMatch(transactionId: String, receiptId: String) {
        updateTransactionMatch(transactionId, receiptId)
    }

    fun clearReceiptMatch(transactionId: String) {
        updateTransactionMatch(transactionId, null)
    }

    fun deleteReceipt(receiptId: String) {
        _uiState.update { state ->
            val receipts = state.dashboard.recentReceipts.filter { it.id != receiptId }
            viewModelScope.launch { repository.deleteReceipt(receiptId) }
            state.copy(
                dashboard = state.dashboard.copy(
                    recentReceipts = receipts,
                    totalSpend = receipts.sumOf { it.total },
                ),
                reports = buildReportsState(
                    receipts = receipts,
                    bankTransactions = state.reports.bankTransactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = state.reports.bankStatementStatus,
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        _uiState.update { state ->
            val transactions = state.reports.bankTransactions.filter { it.id != transactionId }
            viewModelScope.launch { repository.deleteTransaction(transactionId) }
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = transactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = state.reports.bankStatementStatus,
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    fun selectPreviousFinancialYear() {
        moveFinancialYearBy(-1)
    }

    fun selectNextFinancialYear() {
        moveFinancialYearBy(1)
    }

    fun selectCurrentFinancialYear() {
        _uiState.update { state ->
            val currentFinancialYearStart = currentAustralianFinancialYearStart()
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = state.reports.bankTransactions,
                    financialYearStart = currentFinancialYearStart,
                ).copy(
                    bankStatementStatus = "Returned to the current ${financialYearLabel(currentFinancialYearStart)} workspace.",
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    fun setDeterministicFirst(enabled: Boolean) {
        updateSettings { it.copy(deterministicFirst = enabled) }
    }

    fun setMaskSensitiveIdentifiers(enabled: Boolean) {
        updateSettings { it.copy(maskSensitiveIdentifiers = enabled) }
    }

    fun setFallbackOcrProvider(enabled: Boolean) {
        updateSettings { it.copy(fallbackOcrProvider = enabled) }
    }

    fun setInnovationMode(enabled: Boolean) {
        updateSettings { settings ->
            settings.copy(
                innovationMode = enabled,
                deterministicFirst = if (enabled) false else settings.deterministicFirst,
            )
        }
    }

    private fun updateSettings(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { state ->
            val settings = transform(state.settings)
            viewModelScope.launch { settingsDataStore?.save(settings) }
            state.copy(
                settings = settings,
                receipts = state.receipts.copy(
                    deterministicFirst = settings.deterministicFirst,
                    fallbackOcrProvider = settings.fallbackOcrProvider,
                    innovationMode = settings.innovationMode,
                ),
            )
        }
    }

    private fun updateReceiptDraft(transform: (ReceiptReviewDraftUiState) -> ReceiptReviewDraftUiState) {
        _uiState.update { state ->
            state.copy(
                receipts = state.receipts.copy(
                    reviewDraft = transform(state.receipts.reviewDraft),
                ),
            )
        }
    }

    private fun moveFinancialYearBy(delta: Int) {
        _uiState.update { state ->
            val selectedFinancialYearStart = state.reports.selectedFinancialYearStart + delta
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = state.reports.bankTransactions,
                    financialYearStart = selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = "Showing ${financialYearLabel(selectedFinancialYearStart)}. Import statements for this period to update the BAS workspace.",
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    private fun updateTransactionMatch(transactionId: String, receiptId: String?) {
        _uiState.update { state ->
            val existing = state.reports.bankTransactions.firstOrNull { it.id == transactionId } ?: return@update state
            val updated = existing.copy(matchedReceiptId = receiptId)
            val transactions = state.reports.bankTransactions.map { if (it.id == transactionId) updated else it }
            viewModelScope.launch { repository.updateBankTransaction(updated) }
            state.copy(
                reports = buildReportsState(
                    receipts = state.dashboard.recentReceipts,
                    bankTransactions = transactions,
                    financialYearStart = state.reports.selectedFinancialYearStart,
                ).copy(
                    bankStatementStatus = state.reports.bankStatementStatus,
                    importedStatementCount = state.reports.importedStatementCount,
                ),
            )
        }
    }

    private fun createInitialState(): LedgerUiState {
        val receipts = repository.recentReceipts
        val settings = SettingsUiState(
            deterministicFirst = true,
            maskSensitiveIdentifiers = true,
            fallbackOcrProvider = false,
            innovationMode = false,
        )

        return LedgerUiState(
            dashboard = DashboardUiState(
                business = repository.activeBusiness,
                recentReceipts = receipts,
                complianceTasks = repository.complianceTasks,
                totalSpend = receipts.sumOf { it.total },
            ),
            compliance = ComplianceUiState(tasks = repository.complianceTasks),
            reports = buildReportsState(
                receipts = receipts,
                bankTransactions = emptyList(),
                financialYearStart = currentAustralianFinancialYearStart(),
            ),
            receipts = ReceiptUiState(
                selectedImageUri = null,
                sampleText = "Officeworks ABN 36 004 763 526 Receipt OW-2048 Date 12/05/2026 " +
                    "GST $3.86 Total $42.50 printer paper",
                status = "Ready for compressed image upload and local OCR.",
                reviewDraft = ReceiptReviewDraftUiState(),
                deterministicFirst = settings.deterministicFirst,
                fallbackOcrProvider = settings.fallbackOcrProvider,
                innovationMode = settings.innovationMode,
            ),
            settings = settings,
        )
    }

    private fun buildReportsState(
        receipts: List<Receipt>,
        bankTransactions: List<BankTransaction>,
        financialYearStart: Int,
    ): ReportsUiState {
        val financialYearEnd = financialYearStart + 1
        val periods = listOf(
            BasPeriodDefinition(
                id = "q1",
                label = "Quarter 1",
                dateRange = "July-September",
                dueDate = "28 October $financialYearStart",
                start = LocalDate.of(financialYearStart, 7, 1),
                end = LocalDate.of(financialYearStart, 9, 30),
            ),
            BasPeriodDefinition(
                id = "q2",
                label = "Quarter 2",
                dateRange = "October-December",
                dueDate = "28 February $financialYearEnd",
                start = LocalDate.of(financialYearStart, 10, 1),
                end = LocalDate.of(financialYearStart, 12, 31),
            ),
            BasPeriodDefinition(
                id = "q3",
                label = "Quarter 3",
                dateRange = "January-March",
                dueDate = "28 April $financialYearEnd",
                start = LocalDate.of(financialYearEnd, 1, 1),
                end = LocalDate.of(financialYearEnd, 3, 31),
            ),
            BasPeriodDefinition(
                id = "q4",
                label = "Quarter 4",
                dateRange = "April-June",
                dueDate = "28 July $financialYearEnd",
                start = LocalDate.of(financialYearEnd, 4, 1),
                end = LocalDate.of(financialYearEnd, 6, 30),
            ),
        )
        val basPeriods = periods.map { period ->
            val periodReceipts = receipts.filter { it.receiptDate in period.start..period.end }
            val periodTransactions = bankTransactions.filter { it.transactionDate in period.start..period.end }
            BasPeriodUiState(
                id = period.id,
                label = period.label,
                dateRange = period.dateRange,
                startDate = period.start.toString(),
                endDate = period.end.toString(),
                dueDate = period.dueDate,
                receiptCount = periodReceipts.size,
                bankTransactionCount = periodTransactions.size,
                sales = periodTransactions.filter { !it.isExpense }.sumOf { it.amount },
                expenses = periodReceipts.sumOf { it.total } +
                    periodTransactions.filter { it.isExpense }.sumOf { -it.amount },
                gstOnSales = periodTransactions.filter { !it.isExpense }.sumOf { it.gstEstimate },
                gstOnPurchases = periodReceipts.sumOf { it.gst } +
                    periodTransactions.filter { it.isExpense }.sumOf { it.gstEstimate },
                reviewFlagCount = periodTransactions.count { it.needsReview() },
                transactionIds = periodTransactions.map { it.id },
                receiptIds = periodReceipts.map { it.id },
            )
        }
        val financialYearTransactions = bankTransactions.filter {
            it.transactionDate in periods.first().start..periods.last().end
        }
        val financialYearReceipts = receipts.filter {
            it.receiptDate in periods.first().start..periods.last().end
        }
        val transactionsNeedingReview = financialYearTransactions.filter { it.needsReview() }
        val duplicateReceipts = findDuplicateReceipts(financialYearReceipts)
        val receiptMatchSuggestions = findReceiptMatchSuggestions(financialYearReceipts, financialYearTransactions)
        val categoryBreakdown = buildCategoryBreakdown(financialYearReceipts, financialYearTransactions)
        val matchedTransactionCount = financialYearTransactions.count { it.matchedReceiptId != null }
        val unmatchedExpenseCount = financialYearTransactions.count { it.isExpense && it.matchedReceiptId == null }
        val reconciliationSummary = when {
            financialYearTransactions.none { it.isExpense } -> "Import expense transactions to start reconciliation."
            unmatchedExpenseCount == 0 -> "All imported expense transactions have a linked receipt."
            receiptMatchSuggestions.isNotEmpty() -> "$unmatchedExpenseCount expense transactions are unmatched. ${receiptMatchSuggestions.size} likely matches are ready."
            else -> "$unmatchedExpenseCount expense transactions are still waiting for receipt evidence."
        }
        val currentFinancialYearStart = currentAustralianFinancialYearStart()
        val inYearReceipts = basPeriods.sumOf { it.receiptCount }
        val inYearTransactions = basPeriods.sumOf { it.bankTransactionCount }
        val reviewedTransactions = inYearTransactions - basPeriods.sumOf { it.reviewFlagCount }
        val evidenceItemCount = inYearReceipts + inYearTransactions
        val evidenceCoveragePercent = if (evidenceItemCount == 0) {
            0
        } else {
            ((inYearReceipts + reviewedTransactions).toDouble() / evidenceItemCount * 100).toInt()
        }
        val reviewFlagCount = basPeriods.sumOf { it.reviewFlagCount }
        val reportHealthSummary = when {
            evidenceItemCount == 0 -> "No receipts or bank transactions are in this financial year yet."
            reviewFlagCount == 0 -> "All imported evidence in this financial year is categorized with GST estimates."
            else -> "$reviewFlagCount imported transactions need category or GST review before export."
        }
        val smartInsights = buildSmartInsights(
            duplicateReceiptCount = duplicateReceipts.sumOf { it.count - 1 },
            matchSuggestionCount = receiptMatchSuggestions.size,
            topCategory = categoryBreakdown.firstOrNull(),
            reviewFlagCount = reviewFlagCount,
            netGstPosition = basPeriods.sumOf { it.netGst },
        )
        val reviewBrief = ledgerReviewAssistant.buildBrief(
            transactionsNeedingReview = transactionsNeedingReview,
            unmatchedExpenseCount = unmatchedExpenseCount,
            duplicateReceipts = duplicateReceipts,
            receiptMatchSuggestions = receiptMatchSuggestions,
            categoryBreakdown = categoryBreakdown,
            netGstPosition = basPeriods.sumOf { it.netGst },
        )
        val exportSummary = buildExportSummary(
            financialYearLabel = financialYearLabel(financialYearStart),
            evidenceItemCount = evidenceItemCount,
            reviewFlagCount = reviewFlagCount,
            unmatchedExpenseCount = unmatchedExpenseCount,
            duplicateReceiptCount = duplicateReceipts.sumOf { it.count - 1 },
        )
        val exportCsv = buildBasExportCsv(
            financialYearLabel = financialYearLabel(financialYearStart),
            financialYearDateRange = "1 Jul $financialYearStart to 30 Jun $financialYearEnd",
            periods = basPeriods,
            categoryBreakdown = categoryBreakdown,
            transactionsNeedingReview = transactionsNeedingReview,
            duplicateReceipts = duplicateReceipts,
            receiptMatchSuggestions = receiptMatchSuggestions,
        )

        return ReportsUiState(
            selectedFinancialYearStart = financialYearStart,
            currentFinancialYearStart = currentFinancialYearStart,
            financialYearLabel = financialYearLabel(financialYearStart),
            financialYearDateRange = "1 Jul $financialYearStart to 30 Jun $financialYearEnd",
            isCurrentFinancialYear = financialYearStart == currentFinancialYearStart,
            gstOnPurchases = basPeriods.sumOf { it.gstOnPurchases },
            totalTrackedExpenses = basPeriods.sumOf { it.expenses },
            gstOnSales = basPeriods.sumOf { it.gstOnSales },
            totalTrackedSales = basPeriods.sumOf { it.sales },
            netGstPosition = basPeriods.sumOf { it.netGst },
            evidenceCoveragePercent = evidenceCoveragePercent,
            reportHealthSummary = reportHealthSummary,
            reconciliationSummary = reconciliationSummary,
            matchedTransactionCount = matchedTransactionCount,
            unmatchedExpenseCount = unmatchedExpenseCount,
            aiRiskScore = reviewBrief.riskScore,
            aiReviewBrief = reviewBrief.summary,
            aiReviewActions = reviewBrief.actions,
            exportFileName = "ledgeros-bas-${financialYearLabel(financialYearStart).lowercase(Locale.US).replace(" ", "-")}.csv",
            exportCsv = exportCsv,
            exportSummary = exportSummary,
            bankStatementStatus = "Import bank CSV or Excel statements to build ${financialYearLabel(financialYearStart)}.",
            importedStatementCount = 0,
            backlogTransactionCount = financialYearTransactions.size,
            transactionsNeedingReview = transactionsNeedingReview,
            smartInsights = smartInsights,
            categoryBreakdown = categoryBreakdown,
            duplicateReceiptCandidates = duplicateReceipts,
            receiptMatchSuggestions = receiptMatchSuggestions,
            basPeriods = basPeriods,
            bankTransactions = bankTransactions,
        )
    }

    private fun BankTransaction.needsReview(): Boolean {
        return category == "Uncategorised" || gstEstimate == 0.0
    }

    private fun parseDateOrDefault(value: String, default: LocalDate): LocalDate {
        return runCatching { LocalDate.parse(value) }.getOrDefault(default)
    }

    private fun findDuplicateReceipts(receipts: List<Receipt>): List<DuplicateReceiptCandidateUiState> {
        return receipts
            .groupBy {
                "${it.merchant.normalizedKey()}|${it.receiptDate}|${"%.2f".format(Locale.US, it.total)}"
            }
            .values
            .filter { it.size > 1 }
            .map { group ->
                val first = group.first()
                DuplicateReceiptCandidateUiState(
                    receiptIds = group.map { it.id },
                    merchant = first.merchant,
                    date = first.receiptDate.toString(),
                    total = first.total,
                    count = group.size,
                )
            }
            .sortedByDescending { it.count }
            .take(5)
    }

    private fun findReceiptMatchSuggestions(
        receipts: List<Receipt>,
        transactions: List<BankTransaction>,
    ): List<ReceiptMatchSuggestionUiState> {
        return transactions
            .filter { it.isExpense && it.matchedReceiptId == null }
            .mapNotNull { transaction ->
                receipts
                    .mapNotNull { receipt ->
                        val amountDifference = abs(abs(transaction.amount) - receipt.total)
                        val dayDifference = abs(ChronoUnit.DAYS.between(transaction.transactionDate, receipt.receiptDate))
                        val amountTolerance = maxOf(2.0, receipt.total * 0.03)
                        if (amountDifference <= amountTolerance && dayDifference <= 5) {
                            val amountScore = (1.0 - (amountDifference / amountTolerance)).coerceIn(0.0, 1.0)
                            val dateScore = (1.0 - (dayDifference / 5.0)).coerceIn(0.0, 1.0)
                            val nameScore = if (
                                transaction.description.normalizedKey().contains(receipt.merchant.normalizedKey()) ||
                                receipt.merchant.normalizedKey().contains(transaction.description.normalizedKey())
                            ) {
                                0.15
                            } else {
                                0.0
                            }
                            val confidence = ((amountScore * 0.65 + dateScore * 0.25 + nameScore) * 100)
                                .toInt()
                                .coerceIn(40, 99)
                            ReceiptMatchSuggestionUiState(
                                transactionId = transaction.id,
                                transactionDescription = transaction.description,
                                receiptId = receipt.id,
                                receiptMerchant = receipt.merchant,
                                confidencePercent = confidence,
                                amountDifference = amountDifference,
                                dayDifference = dayDifference,
                            )
                        } else {
                            null
                        }
                    }
                    .maxByOrNull { it.confidencePercent }
            }
            .sortedByDescending { it.confidencePercent }
            .take(5)
    }

    private fun buildCategoryBreakdown(
        receipts: List<Receipt>,
        transactions: List<BankTransaction>,
    ): List<CategoryBreakdownUiState> {
        val receiptItems = receipts.map {
            CategoryBreakdownUiState(
                category = it.category.ifBlank { "Uncategorised" },
                amount = it.total,
                gstEstimate = it.gst,
                itemCount = 1,
            )
        }
        val transactionItems = transactions
            .filter { it.isExpense }
            .map {
                CategoryBreakdownUiState(
                    category = it.category.ifBlank { "Uncategorised" },
                    amount = abs(it.amount),
                    gstEstimate = it.gstEstimate,
                    itemCount = 1,
                )
            }

        return (receiptItems + transactionItems)
            .groupBy { it.category }
            .map { (category, items) ->
                CategoryBreakdownUiState(
                    category = category,
                    amount = items.sumOf { it.amount },
                    gstEstimate = items.sumOf { it.gstEstimate },
                    itemCount = items.sumOf { it.itemCount },
                )
            }
            .sortedByDescending { it.amount }
            .take(5)
    }

    private fun buildSmartInsights(
        duplicateReceiptCount: Int,
        matchSuggestionCount: Int,
        topCategory: CategoryBreakdownUiState?,
        reviewFlagCount: Int,
        netGstPosition: Double,
    ): List<String> {
        val insights = mutableListOf<String>()
        if (duplicateReceiptCount > 0) {
            insights += "$duplicateReceiptCount possible duplicate receipts found before BAS export."
        }
        if (matchSuggestionCount > 0) {
            insights += "$matchSuggestionCount likely receipt matches are ready for reconciliation review."
        }
        if (reviewFlagCount > 0) {
            insights += "$reviewFlagCount transactions need GST or category cleanup."
        }
        if (topCategory != null) {
            insights += "${topCategory.category} is the largest spend category at ${"%.0f".format(Locale.US, topCategory.amount)}."
        }
        insights += if (netGstPosition >= 0.0) {
            "Current estimate is a GST payable position before final review."
        } else {
            "Current estimate is a GST credit position before final review."
        }
        return insights.take(5)
    }

    private fun buildExportSummary(
        financialYearLabel: String,
        evidenceItemCount: Int,
        reviewFlagCount: Int,
        unmatchedExpenseCount: Int,
        duplicateReceiptCount: Int,
    ): String {
        val blockers = listOf(
            reviewFlagCount.takeIf { it > 0 }?.let { "$it category/GST reviews" },
            unmatchedExpenseCount.takeIf { it > 0 }?.let { "$it unmatched expenses" },
            duplicateReceiptCount.takeIf { it > 0 }?.let { "$it possible duplicate receipts" },
        ).filterNotNull()

        return if (evidenceItemCount == 0) {
            "$financialYearLabel export pack is empty until receipts or bank transactions are added."
        } else if (blockers.isEmpty()) {
            "$financialYearLabel export pack is ready for human review."
        } else {
            "$financialYearLabel export pack prepared with ${blockers.joinToString(", ")} still open."
        }
    }

    private fun buildBasExportCsv(
        financialYearLabel: String,
        financialYearDateRange: String,
        periods: List<BasPeriodUiState>,
        categoryBreakdown: List<CategoryBreakdownUiState>,
        transactionsNeedingReview: List<BankTransaction>,
        duplicateReceipts: List<DuplicateReceiptCandidateUiState>,
        receiptMatchSuggestions: List<ReceiptMatchSuggestionUiState>,
    ): String {
        val rows = mutableListOf<List<String>>()
        rows += listOf("LedgerOS BAS preparation export")
        rows += listOf("Financial year", financialYearLabel)
        rows += listOf("Date range", financialYearDateRange)
        rows.add(emptyList())
        rows += listOf(
            "Quarter",
            "Date range",
            "Due date",
            "Sales",
            "Expenses",
            "GST on sales",
            "GST on purchases",
            "Net GST",
            "Receipts",
            "Bank transactions",
            "Review flags",
        )
        periods.forEach { period ->
            rows += listOf(
                period.label,
                "${period.startDate} to ${period.endDate}",
                period.dueDate,
                period.sales.toCsvMoney(),
                period.expenses.toCsvMoney(),
                period.gstOnSales.toCsvMoney(),
                period.gstOnPurchases.toCsvMoney(),
                period.netGst.toCsvMoney(),
                period.receiptCount.toString(),
                period.bankTransactionCount.toString(),
                period.reviewFlagCount.toString(),
            )
        }
        rows.add(emptyList())
        rows += listOf("Top spend categories")
        rows += listOf("Category", "Amount", "GST estimate", "Items")
        categoryBreakdown.forEach { category ->
            rows += listOf(
                category.category,
                category.amount.toCsvMoney(),
                category.gstEstimate.toCsvMoney(),
                category.itemCount.toString(),
            )
        }
        rows.add(emptyList())
        rows += listOf("Open review items")
        rows += listOf("Date", "Description", "Category", "Amount", "GST estimate")
        transactionsNeedingReview.forEach { transaction ->
            rows += listOf(
                transaction.transactionDate.toString(),
                transaction.description,
                transaction.category,
                transaction.amount.toCsvMoney(),
                transaction.gstEstimate.toCsvMoney(),
            )
        }
        rows.add(emptyList())
        rows += listOf("Possible duplicate receipts")
        rows += listOf("Merchant", "Date", "Total", "Count")
        duplicateReceipts.forEach { duplicate ->
            rows += listOf(
                duplicate.merchant,
                duplicate.date,
                duplicate.total.toCsvMoney(),
                duplicate.count.toString(),
            )
        }
        rows.add(emptyList())
        rows += listOf("Suggested receipt matches")
        rows += listOf("Transaction", "Receipt merchant", "Confidence", "Amount difference", "Day difference")
        receiptMatchSuggestions.forEach { suggestion ->
            rows += listOf(
                suggestion.transactionDescription,
                suggestion.receiptMerchant,
                "${suggestion.confidencePercent}%",
                suggestion.amountDifference.toCsvMoney(),
                suggestion.dayDifference.toString(),
            )
        }
        rows.add(emptyList())
        rows += listOf("Note", "Preparation and record management only. Review figures before lodging through official channels or a registered adviser.")

        return rows.joinToString("\n") { row ->
            row.joinToString(",") { cell -> cell.csvEscaped() }
        }
    }

    private fun Double.toCsvMoney(): String {
        return String.format(Locale.US, "%.2f", this)
    }

    private fun String.csvEscaped(): String {
        val escaped = replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun String.normalizedKey(): String {
        return lowercase(Locale.US).filter { it.isLetterOrDigit() }
    }

    private fun currentAustralianFinancialYearStart(
        today: LocalDate = LocalDate.now(ZoneId.of("Australia/Sydney")),
    ): Int {
        return if (today.month >= Month.JULY) today.year else today.year - 1
    }

    private fun financialYearLabel(startYear: Int): String {
        return "FY $startYear-${(startYear + 1).toString().takeLast(2)}"
    }

    private data class BasPeriodDefinition(
        val id: String,
        val label: String,
        val dateRange: String,
        val dueDate: String,
        val start: LocalDate,
        val end: LocalDate,
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                if (application != null) {
                    val ctx = application.applicationContext
                    LedgerViewModel(
                        repository = RoomLedgerRepository(ctx),
                        settingsDataStore = SettingsDataStore(ctx),
                        localOcrService = MlKitReceiptOcrService(ctx),
                    )
                } else {
                    LedgerViewModel(DemoLedgerRepository())
                }
            }
        }
    }
}
