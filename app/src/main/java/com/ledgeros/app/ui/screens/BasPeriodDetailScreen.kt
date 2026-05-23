package com.ledgeros.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.Receipt
import com.ledgeros.app.ui.state.BasPeriodUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BasPeriodDetailScreen(
    period: BasPeriodUiState,
    transactions: List<BankTransaction>,
    receipts: List<Receipt>,
    onTransactionClick: (String) -> Unit,
    onReceiptClick: (String) -> Unit,
) {
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))
    val netGstColor = if (period.netGst >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "${period.label}: ${period.dateRange}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${period.startDate} – ${period.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Due ${period.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.elevatedCardElevation(2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    SummaryColumn("Sales", currency.format(period.sales), MaterialTheme.colorScheme.primary)
                    SummaryColumn("Expenses", currency.format(period.expenses), MaterialTheme.colorScheme.onSurface)
                    SummaryColumn("Net GST", currency.format(period.netGst), netGstColor)
                }
                if (period.reviewFlagCount > 0) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            "  ${period.reviewFlagCount} transaction${if (period.reviewFlagCount > 1) "s" else ""} need review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("Transactions (${transactions.size})")
        }
        if (transactions.isEmpty()) {
            item {
                EmptySection("No transactions for this quarter")
            }
        } else {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(2.dp),
                ) {
                    transactions.forEachIndexed { index, transaction ->
                        val needsReview = transaction.category == "Uncategorised" || transaction.gstEstimate == 0.0
                        ListItem(
                            leadingContent = {
                                Icon(
                                    if (needsReview) Icons.Outlined.WarningAmber else Icons.Outlined.AccountBalance,
                                    contentDescription = null,
                                    tint = if (needsReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                )
                            },
                            headlineContent = { Text(transaction.description, style = MaterialTheme.typography.bodyMedium) },
                            supportingContent = {
                                Text(
                                    "${transaction.transactionDate} · ${transaction.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        currency.format(transaction.amount),
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (transaction.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.clickable { onTransactionClick(transaction.id) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        if (index < transactions.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        item {
            SectionLabel("Receipts (${receipts.size})")
        }
        if (receipts.isEmpty()) {
            item {
                EmptySection("No receipts for this quarter")
            }
        } else {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(2.dp),
                ) {
                    receipts.forEachIndexed { index, receipt ->
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Outlined.Receipt, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            headlineContent = { Text(receipt.merchant, style = MaterialTheme.typography.bodyMedium) },
                            supportingContent = {
                                Text(
                                    "${receipt.receiptDate} · ${receipt.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        currency.format(receipt.total),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.clickable { onReceiptClick(receipt.id) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        if (index < receipts.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun EmptySection(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
