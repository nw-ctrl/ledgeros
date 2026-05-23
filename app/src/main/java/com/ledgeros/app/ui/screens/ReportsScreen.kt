package com.ledgeros.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledgeros.app.ui.components.QuarterBarChart
import com.ledgeros.app.ui.state.BasPeriodUiState
import com.ledgeros.app.ui.state.CategoryBreakdownUiState
import com.ledgeros.app.ui.state.DuplicateReceiptCandidateUiState
import com.ledgeros.app.ui.state.ReceiptMatchSuggestionUiState
import com.ledgeros.app.ui.state.ReportsUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onBankStatementImported: (String, ByteArray) -> Unit,
    onManualAdjustmentSaved: (String, String, String, String, String) -> Unit,
    onBasPeriodClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onPreviousFinancialYearClick: () -> Unit,
    onCurrentFinancialYearClick: () -> Unit,
    onNextFinancialYearClick: () -> Unit,
) {
    val context = LocalContext.current
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))
    var adjustmentDate by rememberSaveable { mutableStateOf("") }
    var adjustmentDescription by rememberSaveable { mutableStateOf("") }
    var adjustmentAmount by rememberSaveable { mutableStateOf("") }
    var adjustmentCategory by rememberSaveable { mutableStateOf("") }
    var adjustmentGst by rememberSaveable { mutableStateOf("") }
    var showManualForm by rememberSaveable { mutableStateOf(false) }

    val statementPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            onBankStatementImported(it.toString(), context.readBytesFromUri(it))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Financial year selector
        item {
            FinancialYearHeader(
                label = uiState.financialYearLabel,
                dateRange = uiState.financialYearDateRange,
                isCurrentYear = uiState.isCurrentFinancialYear,
                onPrevious = onPreviousFinancialYearClick,
                onCurrent = onCurrentFinancialYearClick,
                onNext = onNextFinancialYearClick,
            )
        }

        // Summary metrics
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryChip(
                    label = "GST on purchases",
                    value = currency.format(uiState.gstOnPurchases),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                SummaryChip(
                    label = "GST on sales",
                    value = currency.format(uiState.gstOnSales),
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryChip(
                    label = "Net GST position",
                    value = currency.format(uiState.netGstPosition),
                    color = if (uiState.netGstPosition >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                SummaryChip(
                    label = "Export readiness",
                    value = "${uiState.evidenceCoveragePercent}%",
                    color = when {
                        uiState.evidenceCoveragePercent >= 90 -> MaterialTheme.colorScheme.primary
                        uiState.evidenceCoveragePercent >= 60 -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Quarterly bar chart
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Quarterly expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        uiState.financialYearLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    QuarterBarChart(
                        labels = uiState.basPeriods.map { it.label.take(2) },
                        values = uiState.basPeriods.map { it.expenses },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // AI review brief
        item {
            SectionHeader("AI review brief")
        }
        item {
            AiReviewCard(
                riskScore = uiState.aiRiskScore,
                summary = uiState.aiReviewBrief,
                actions = uiState.aiReviewActions,
            )
        }

        // Reconciliation
        item {
            SectionHeader("Reconciliation")
        }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                ListItem(
                    leadingContent = {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    headlineContent = { Text("Reconciliation progress") },
                    supportingContent = { Text(uiState.reconciliationSummary) },
                    trailingContent = {
                        Text(
                            "${uiState.matchedTransactionCount}/${uiState.matchedTransactionCount + uiState.unmatchedExpenseCount}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        // Smart insights
        if (uiState.smartInsights.isNotEmpty()) {
            item { SectionHeader("Smart insights") }
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    uiState.smartInsights.forEachIndexed { index, insight ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        if (index < uiState.smartInsights.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        // Suggested receipt matches
        if (uiState.receiptMatchSuggestions.isNotEmpty()) {
            item { SectionHeader("Suggested matches") }
            items(uiState.receiptMatchSuggestions) { suggestion ->
                MatchSuggestionCard(
                    suggestion = suggestion,
                    currency = currency,
                    onClick = { onTransactionClick(suggestion.transactionId) },
                )
            }
        }

        // Duplicate receipts
        if (uiState.duplicateReceiptCandidates.isNotEmpty()) {
            item { SectionHeader("Possible duplicates") }
            items(uiState.duplicateReceiptCandidates) { duplicate ->
                DuplicateReceiptCard(duplicate = duplicate, currency = currency)
            }
        }

        // Category breakdown
        if (uiState.categoryBreakdown.isNotEmpty()) {
            item { SectionHeader("Top spend categories") }
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    uiState.categoryBreakdown.forEachIndexed { index, cat ->
                        CategoryRow(category = cat, currency = currency)
                        if (index < uiState.categoryBreakdown.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        // Quarterly sections
        item { SectionHeader("Quarterly breakdown") }
        items(uiState.basPeriods) { period ->
            QuarterPeriodCard(
                period = period,
                currency = currency,
                onClick = { onBasPeriodClick(period.id) },
            )
        }

        // Review queue
        if (uiState.transactionsNeedingReview.isNotEmpty()) {
            item { SectionHeader("Review queue (${uiState.transactionsNeedingReview.size})") }
            items(uiState.transactionsNeedingReview.take(5)) { transaction ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onTransactionClick(transaction.id) },
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = { Text(transaction.description) },
                        supportingContent = {
                            Text("${transaction.transactionDate} · ${transaction.category} · GST ${currency.format(transaction.gstEstimate)}")
                        },
                        trailingContent = {
                            Text(
                                currency.format(transaction.amount),
                                fontWeight = FontWeight.SemiBold,
                                color = if (transaction.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }

        // Import tools
        item { SectionHeader("Import & tools") }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        statementPicker.launch(
                            arrayOf("text/*", "text/csv", "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.TableChart, contentDescription = null)
                    Text("  Import CSV or Excel statement")
                }

                if (uiState.bankStatementStatus.isNotBlank()) {
                    Text(
                        uiState.bankStatementStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Manual adjustment form
        item {
            ManualAdjustmentCard(
                expanded = showManualForm,
                onExpandToggle = { showManualForm = !showManualForm },
                date = adjustmentDate,
                description = adjustmentDescription,
                amount = adjustmentAmount,
                category = adjustmentCategory,
                gst = adjustmentGst,
                onDateChange = { adjustmentDate = it },
                onDescriptionChange = { adjustmentDescription = it },
                onAmountChange = { adjustmentAmount = it },
                onCategoryChange = { adjustmentCategory = it },
                onGstChange = { adjustmentGst = it },
                onSave = {
                    onManualAdjustmentSaved(adjustmentDate, adjustmentDescription, adjustmentAmount, adjustmentCategory, adjustmentGst)
                    adjustmentDate = ""; adjustmentDescription = ""; adjustmentAmount = ""; adjustmentCategory = ""; adjustmentGst = ""
                    showManualForm = false
                },
            )
        }

        // Export
        item { SectionHeader("BAS export") }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                ListItem(
                    leadingContent = {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    headlineContent = { Text("BAS export pack") },
                    supportingContent = { Text(uiState.exportSummary) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                OutlinedButton(
                    onClick = { context.shareBasExport(fileName = uiState.exportFileName, csv = uiState.exportCsv) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                    Text("  Share BAS CSV export")
                }
            }
        }

        item {
            Text(
                "Preparation only. Review with a registered adviser before lodging.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FinancialYearHeader(
    label: String,
    dateRange: String,
    isCurrentYear: Boolean,
    onPrevious: () -> Unit,
    onCurrent: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous year")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    dateRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isCurrentYear) {
                    Text(
                        "Historical workspace",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                    )
                }
            }
            IconButton(onClick = onCurrent, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Today, contentDescription = "Current year", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next year")
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun AiReviewCard(riskScore: Int, summary: String, actions: List<String>) {
    val riskColor = when {
        riskScore >= 70 -> MaterialTheme.colorScheme.error
        riskScore >= 40 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("AI review brief", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Surface(shape = MaterialTheme.shapes.extraSmall, color = riskColor.copy(alpha = 0.12f)) {
                    Text(
                        "Risk: $riskScore/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(summary, style = MaterialTheme.typography.bodySmall)
            }
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                actions.forEach { action ->
                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                        Text("• ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(action, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarterPeriodCard(
    period: BasPeriodUiState,
    currency: NumberFormat,
    onClick: () -> Unit,
) {
    val netGstColor = if (period.netGst >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${period.label}: ${period.dateRange}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Due ${period.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Exp: ${currency.format(period.expenses)}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "Sales: ${currency.format(period.sales)}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (period.reviewFlagCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${period.reviewFlagCount} review flags",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currency.format(period.netGst),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = netGstColor,
                )
                Text(
                    "net GST",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryBreakdownUiState, currency: NumberFormat) {
    ListItem(
        leadingContent = {
            Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        headlineContent = { Text(category.category, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Text(
                "${category.itemCount} items · GST ${currency.format(category.gstEstimate)}",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Text(
                currency.format(category.amount),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun MatchSuggestionCard(
    suggestion: ReceiptMatchSuggestionUiState,
    currency: NumberFormat,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            leadingContent = {
                Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            headlineContent = {
                Text(
                    "${suggestion.confidencePercent}% match: ${suggestion.receiptMerchant}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                Text(
                    "${suggestion.transactionDescription}\n" +
                        "Δ ${currency.format(suggestion.amountDifference)} / ${suggestion.dayDifference} days",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun DuplicateReceiptCard(
    duplicate: DuplicateReceiptCandidateUiState,
    currency: NumberFormat,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            leadingContent = {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            headlineContent = {
                Text(
                    "${duplicate.count} copies: ${duplicate.merchant}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                Text(
                    "${duplicate.date} · ${currency.format(duplicate.total)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun ManualAdjustmentCard(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    date: String,
    description: String,
    amount: String,
    category: String,
    gst: String,
    onDateChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onGstChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            leadingContent = {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            headlineContent = { Text("Manual adjustment") },
            supportingContent = { Text("Add a transaction not in your bank import.") },
            trailingContent = {
                Icon(
                    if (expanded) Icons.AutoMirrored.Outlined.KeyboardArrowLeft else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable(onClick = onExpandToggle),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )

        if (expanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = onDateChange,
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (-88.00 expense / 2420.00 sale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = gst,
                    onValueChange = onGstChange,
                    label = { Text("GST estimate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null)
                    Text("  Add adjustment")
                }
            }
        }
    }
}

private fun android.content.Context.readBytesFromUri(uri: Uri): ByteArray {
    return contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
}

private fun android.content.Context.shareBasExport(fileName: String, csv: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TITLE, fileName)
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    startActivity(Intent.createChooser(intent, "Share BAS export"))
}
