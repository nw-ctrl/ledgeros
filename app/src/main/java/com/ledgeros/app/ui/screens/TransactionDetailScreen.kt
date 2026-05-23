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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.Receipt
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun TransactionDetailScreen(
    transaction: BankTransaction,
    matchedReceipt: Receipt?,
    suggestedReceipt: Receipt?,
    onReceiptClick: (String) -> Unit,
    onSaveClick: (String, String, String, String, String) -> Unit,
    onAcceptReceiptMatch: (String) -> Unit,
    onClearReceiptMatch: () -> Unit,
    onDeleteClick: () -> Unit = {},
) {
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))
    val needsReview = transaction.category == "Uncategorised" || transaction.gstEstimate == 0.0
    var description by remember(transaction.id) { mutableStateOf(transaction.description) }
    var transactionDate by remember(transaction.id) { mutableStateOf(transaction.transactionDate.toString()) }
    var amount by remember(transaction.id) { mutableStateOf("%.2f".format(transaction.amount)) }
    var category by remember(transaction.id) { mutableStateOf(transaction.category) }
    var gstEstimate by remember(transaction.id) { mutableStateOf("%.2f".format(transaction.gstEstimate)) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete transaction?") },
            text = {
                Text("This will permanently remove \"${transaction.description}\" · ${currency.format(transaction.amount)} from your records.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = {
                        Text(transaction.description, fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Spacer(Modifier.height(4.dp))
                            Text("${transaction.transactionDate}", style = MaterialTheme.typography.bodySmall)
                            Text("${transaction.category}", style = MaterialTheme.typography.bodySmall)
                            Text("GST ${currency.format(transaction.gstEstimate)}", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    trailingContent = {
                        Text(
                            currency.format(transaction.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        if (needsReview) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = { Text("Needs review", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text("Category or GST is missing. Edit before using in BAS preparation.")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }

        item {
            Text("Edit fields", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = transactionDate,
                onValueChange = { transactionDate = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                supportingText = { Text("Expenses: negative · Sales: positive") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = gstEstimate,
                    onValueChange = { gstEstimate = it },
                    label = { Text("GST estimate") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
        item {
            Button(
                onClick = { onSaveClick(description, transactionDate, amount, category, gstEstimate) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Save changes")
            }
        }

        // Receipt matching
        item {
            Text("Linked receipt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        item {
            if (matchedReceipt != null) {
                LinkedReceiptCard(
                    receipt = matchedReceipt,
                    currency = currency,
                    transaction = transaction,
                    onReceiptClick = onReceiptClick,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onClearReceiptMatch, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Clear receipt match")
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ) {
                    ListItem(
                        headlineContent = { Text("No receipt matched yet") },
                        supportingContent = {
                            Text("Reconcile this transaction by linking it to a receipt below, or scan a receipt in the Receipts tab.")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }

        if (matchedReceipt == null && suggestedReceipt != null) {
            item {
                Text("Suggested match", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            item {
                LinkedReceiptCard(
                    receipt = suggestedReceipt,
                    currency = currency,
                    transaction = transaction,
                    onReceiptClick = onReceiptClick,
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onAcceptReceiptMatch(suggestedReceipt.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Accept match")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Delete transaction")
            }
        }
    }
}

@Composable
private fun LinkedReceiptCard(
    receipt: Receipt,
    currency: NumberFormat,
    transaction: BankTransaction,
    onReceiptClick: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(receipt.id) },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            leadingContent = {
                Icon(Icons.Outlined.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            headlineContent = { Text(receipt.merchant, fontWeight = FontWeight.Medium) },
            supportingContent = {
                Text("${receipt.receiptDate} · ${receipt.category}\nΔ ${currency.format(abs(abs(transaction.amount) - receipt.total))}")
            },
            trailingContent = {
                Text(
                    currency.format(receipt.total),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
