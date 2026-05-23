package com.ledgeros.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ledgeros.app.model.BasFrequency
import com.ledgeros.app.ui.state.PremiumTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (
        businessName: String,
        abn: String,
        gstRegistered: Boolean,
        basFrequency: BasFrequency,
        startPremium: Boolean,
    ) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var businessName by rememberSaveable { mutableStateOf("") }
    var abn by rememberSaveable { mutableStateOf("") }
    var gstRegistered by rememberSaveable { mutableStateOf(true) }
    var basFrequency by rememberSaveable { mutableStateOf(BasFrequency.Quarterly) }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it / 6 })
                .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { -it / 6 })
        },
        label = "onboarding_step",
    ) { currentStep ->
        when (currentStep) {
            0 -> WelcomeStep(onNext = { step = 1 })
            1 -> BusinessSetupStep(
                businessName = businessName,
                abn = abn,
                gstRegistered = gstRegistered,
                basFrequency = basFrequency,
                onBusinessNameChange = { businessName = it },
                onAbnChange = { abn = it },
                onGstChange = { gstRegistered = it },
                onBasFrequencyChange = { basFrequency = it },
                onBack = { step = 0 },
                onNext = { step = 2 },
            )
            2 -> ChoosePlanStep(
                onFree = { onComplete(businessName, abn, gstRegistered, basFrequency, false) },
                onPro = { onComplete(businessName, abn, gstRegistered, basFrequency, true) },
                onBack = { step = 1 },
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val gradient = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "LedgerOS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "BAS prep. Done properly.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            FeatureRow(
                icon = Icons.Outlined.DocumentScanner,
                title = "On-device OCR",
                detail = "Scan receipts instantly — no cloud upload, full privacy.",
            )
            FeatureRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "Smart BAS workspace",
                detail = "Australian quarterly periods, GST calculations and export pack built in.",
            )
            FeatureRow(
                icon = Icons.Outlined.CheckCircle,
                title = "Reconciliation engine",
                detail = "Automatically matches bank transactions to scanned receipts.",
            )
            FeatureRow(
                icon = Icons.Outlined.Star,
                title = "AI review brief",
                detail = "Risk score, duplicate detection and open review flags before lodgement.",
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Get started", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessSetupStep(
    businessName: String,
    abn: String,
    gstRegistered: Boolean,
    basFrequency: BasFrequency,
    onBusinessNameChange: (String) -> Unit,
    onAbnChange: (String) -> Unit,
    onGstChange: (Boolean) -> Unit,
    onBasFrequencyChange: (BasFrequency) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var frequencyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepHeader(step = 2, total = 3, title = "Your business")
        Text(
            "This sets up your BAS workspace. You can update details in Settings later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = businessName,
            onValueChange = onBusinessNameChange,
            label = { Text("Business or trading name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = abn,
            onValueChange = { if (it.length <= 11) onAbnChange(it.filter { c -> c.isDigit() }) },
            label = { Text("ABN (11 digits)") },
            supportingText = { Text("Leave blank to add later") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        ElevatedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GST registered", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Enables GST tracking on receipts and transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = gstRegistered, onCheckedChange = onGstChange)
            }
        }

        ExposedDropdownMenuBox(
            expanded = frequencyExpanded,
            onExpandedChange = { frequencyExpanded = it },
        ) {
            OutlinedTextField(
                value = when (basFrequency) {
                    BasFrequency.Quarterly -> "Quarterly (most common)"
                    BasFrequency.Monthly -> "Monthly"
                    BasFrequency.Annually -> "Annually"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("BAS lodgement frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = frequencyExpanded,
                onDismissRequest = { frequencyExpanded = false },
            ) {
                listOf(
                    BasFrequency.Quarterly to "Quarterly (most common)",
                    BasFrequency.Monthly to "Monthly",
                    BasFrequency.Annually to "Annually",
                ).forEach { (freq, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onBasFrequencyChange(freq)
                            frequencyExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = businessName.isNotBlank(),
        ) {
            Text("Continue")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun ChoosePlanStep(
    onFree: () -> Unit,
    onPro: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(step = 3, total = 3, title = "Choose your plan")
        Text(
            "Start free and upgrade any time from Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Free plan
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(2.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Free", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("A\$0 / forever", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                PlanFeature("Up to ${PremiumTier.FREE_RECEIPT_LIMIT} receipts stored")
                PlanFeature("Current financial year only")
                PlanFeature("BAS CSV export")
                PlanFeature("On-device OCR scanning")
                PlanFeatureLocked("Multi-year history")
                PlanFeatureLocked("BAS due-date notifications")
                PlanFeatureLocked("Unlimited receipts")
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onFree, modifier = Modifier.fillMaxWidth()) {
                    Text("Start free")
                }
            }
        }

        // Pro plan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(PremiumTier.PRO_MONTHLY_PRICE, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(" / month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("or ${PremiumTier.PRO_YEARLY_PRICE}/yr · ${PremiumTier.PRO_YEARLY_SAVING}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                PlanFeature("Unlimited receipts", highlighted = true)
                PlanFeature("All financial years", highlighted = true)
                PlanFeature("BAS due-date notifications", highlighted = true)
                PlanFeature("Multi-business (coming soon)", highlighted = true)
                PlanFeature("Priority support", highlighted = true)
                PlanFeature("BAS CSV export")
                PlanFeature("On-device OCR scanning")
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPro,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Start Pro — ${PremiumTier.PRO_MONTHLY_PRICE}/month")
                }
                Text(
                    "7-day free trial · Cancel any time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun PlanFeature(text: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun PlanFeatureLocked(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.WorkspacePremium,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepHeader(step: Int, total: Int, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        ),
                )
            }
        }
        Text("Step $step of $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}
