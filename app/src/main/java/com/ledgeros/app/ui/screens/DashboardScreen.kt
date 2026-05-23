package com.ledgeros.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledgeros.app.model.Receipt
import com.ledgeros.app.ui.components.ChartSegment
import com.ledgeros.app.ui.components.MetricCard
import com.ledgeros.app.ui.components.SpendingDonutChart
import com.ledgeros.app.ui.state.DashboardUiState
import com.ledgeros.app.ui.theme.chartColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(uiState: DashboardUiState) {
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))

    val categorySegments = uiState.recentReceipts
        .groupBy { it.category.ifBlank { "Uncategorised" } }
        .map { (cat, receipts) -> ChartSegment(cat, receipts.sumOf { it.total }.toFloat()) }
        .sortedByDescending { it.value }
        .take(6)

    val overdueTasks = uiState.complianceTasks.count { it.isOverdue }
    val pendingTasks = uiState.complianceTasks.count { it.status != com.ledgeros.app.model.ComplianceStatus.Done }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Hero header
        item {
            HeroCard(
                businessName = uiState.business.businessName,
                abn = uiState.business.abn,
                totalSpend = uiState.totalSpend,
                currency = currency,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Spending donut chart
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Spend by category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(16.dp))
                    SpendingDonutChart(
                        segments = categorySegments,
                        total = uiState.totalSpend,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Metrics row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    label = "Receipts",
                    value = uiState.recentReceipts.size.toString(),
                    icon = Icons.Outlined.Receipt,
                    modifier = Modifier.weight(1f),
                    accentColor = chartColors[0],
                )
                MetricCard(
                    label = "Open tasks",
                    value = pendingTasks.toString(),
                    icon = Icons.Outlined.EventAvailable,
                    modifier = Modifier.weight(1f),
                    accentColor = if (overdueTasks > 0) MaterialTheme.colorScheme.error else chartColors[1],
                    supportText = if (overdueTasks > 0) "$overdueTasks overdue" else if (pendingTasks == 0) "All done" else null,
                )
            }
        }

        // Recent receipts section
        item {
            Text(
                text = "Recent receipts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (uiState.recentReceipts.isEmpty()) {
            item {
                EmptyReceiptsCard()
            }
        } else {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    uiState.recentReceipts.take(8).forEachIndexed { index, receipt ->
                        ReceiptRow(receipt = receipt, currency = currency)
                        if (index < uiState.recentReceipts.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    businessName: String,
    abn: String,
    totalSpend: Double,
    currency: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(gradientColors))
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "  $businessName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (abn.isNotBlank()) {
                Text(
                    text = "ABN ${abn.chunked(3).joinToString(" ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 2.dp, start = 26.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tracked spend",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
            )
            Text(
                text = currency.format(totalSpend),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReceiptRow(receipt: Receipt, currency: NumberFormat) {
    val colorIndex = receipt.category.hashCode().and(0x7FFFFFFF) % chartColors.size
    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(chartColors[colorIndex].copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = chartColors[colorIndex],
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        headlineContent = {
            Text(
                text = receipt.merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Text(
                text = receipt.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(
                text = currency.format(receipt.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun EmptyReceiptsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No receipts yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Scan your first receipt in the Receipts tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
