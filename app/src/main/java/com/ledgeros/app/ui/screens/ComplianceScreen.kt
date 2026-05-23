package com.ledgeros.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.model.ComplianceTask
import com.ledgeros.app.ui.state.ComplianceUiState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ComplianceScreen(
    uiState: ComplianceUiState,
    onStatusChange: (taskId: String, status: ComplianceStatus) -> Unit,
) {
    val overdueCount = uiState.tasks.count { it.isOverdue }
    val doneCount = uiState.tasks.count { it.status == ComplianceStatus.Done }
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Summary strip
        item {
            ComplianceSummaryRow(
                total = uiState.tasks.size,
                done = doneCount,
                overdue = overdueCount,
            )
        }

        if (overdueCount > 0) {
            item { OverdueBanner(count = overdueCount) }
        }

        if (uiState.tasks.isEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No compliance tasks yet", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Tasks are seeded from your business profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Active tasks first, then done
        val ordered = uiState.tasks.sortedWith(
            compareBy(
                { it.status == ComplianceStatus.Done },
                { it.dueDate },
            ),
        )

        items(ordered) { task ->
            ComplianceTaskCard(task = task, today = today, onStatusChange = onStatusChange)
        }
    }
}

@Composable
private fun ComplianceSummaryRow(total: Int, done: Int, overdue: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryPill(label = "Total", value = total.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        SummaryPill(label = "Done", value = done.toString(), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        SummaryPill(
            label = "Overdue",
            value = overdue.toString(),
            color = if (overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, elevation = CardDefaults.elevatedCardElevation(2.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OverdueBanner(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "$count task${if (count > 1) "s" else ""} overdue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    "Review and action these as soon as possible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun ComplianceTaskCard(
    task: ComplianceTask,
    today: LocalDate,
    onStatusChange: (String, ComplianceStatus) -> Unit,
) {
    val daysUntilDue = ChronoUnit.DAYS.between(today, task.dueDate)
    val isOverdue = task.isOverdue
    val isDueSoon = !isOverdue && daysUntilDue in 0..14
    val isDone = task.status == ComplianceStatus.Done

    val statusColor = when {
        isDone -> MaterialTheme.colorScheme.primary
        isOverdue -> MaterialTheme.colorScheme.error
        isDueSoon -> Color(0xFFE65100)
        task.status == ComplianceStatus.Pending -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(300),
        label = "task_color",
    )

    val statusIcon = when {
        isDone -> Icons.Outlined.CheckCircle
        isOverdue -> Icons.Outlined.WarningAmber
        isDueSoon -> Icons.Outlined.HourglassBottom
        task.status == ComplianceStatus.Pending -> Icons.Outlined.Schedule
        else -> Icons.Outlined.RadioButtonUnchecked
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDone) 1.dp else 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(statusIcon, contentDescription = null, tint = animatedColor, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.taskType.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Due ${task.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when {
                        isOverdue -> Text(
                            "${-daysUntilDue} day${if (-daysUntilDue != 1L) "s" else ""} overdue",
                            style = MaterialTheme.typography.labelSmall,
                            color = animatedColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        isDueSoon && !isDone -> Text(
                            if (daysUntilDue == 0L) "Due today" else "Due in $daysUntilDue day${if (daysUntilDue != 1L) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = animatedColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = animatedColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = task.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Action buttons
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (task.status) {
                    ComplianceStatus.Todo -> {
                        FilledTonalButton(
                            onClick = { onStatusChange(task.id, ComplianceStatus.Pending) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp))
                            Text("  Mark pending", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onStatusChange(task.id, ComplianceStatus.Done) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Text("  Mark done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    ComplianceStatus.Pending -> {
                        OutlinedButton(
                            onClick = { onStatusChange(task.id, ComplianceStatus.Todo) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.History, null, modifier = Modifier.size(16.dp))
                            Text("  Reset to do", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onStatusChange(task.id, ComplianceStatus.Done) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Text("  Mark done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    ComplianceStatus.Done -> {
                        OutlinedButton(
                            onClick = { onStatusChange(task.id, ComplianceStatus.Todo) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        ) {
                            Icon(Icons.Outlined.History, null, modifier = Modifier.size(16.dp))
                            Text("  Reopen task", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
