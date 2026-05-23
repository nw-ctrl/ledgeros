package com.ledgeros.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.text.style.TextDecoration
import com.ledgeros.app.model.GrantedTier
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledgeros.app.ui.state.PremiumTier
import com.ledgeros.app.ui.state.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    isPremium: Boolean = false,
    isAuthenticated: Boolean = false,
    isOwner: Boolean = false,
    grantedTier: GrantedTier? = null,
    onDeterministicFirstChange: (Boolean) -> Unit,
    onMaskSensitiveIdentifiersChange: (Boolean) -> Unit,
    onFallbackOcrProviderChange: (Boolean) -> Unit,
    onInnovationModeChange: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit = {},
    onRestorePurchasesClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onManageAccessClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Subscription card ────────────────────────────────────────────
        item {
            when {
                isOwner -> OwnerAccessCard(onManageAccessClick = onManageAccessClick)
                grantedTier != null && grantedTier != GrantedTier.Free ->
                    GrantedProCard(tier = grantedTier)
                isPremium -> ProActiveCard()
                else -> ProUpgradeSettingsCard(
                    onUpgradeClick = onUpgradeClick,
                    onRestoreClick = onRestorePurchasesClick,
                )
            }
        }

        // ── Data extraction ──────────────────────────────────────────────
        item {
            SettingsGroupCard(title = "Data extraction", icon = Icons.Outlined.Tune) {
                SettingsToggleRow(
                    title = "Prefer deterministic parsing",
                    description = "Use regex-based extraction before any adaptive methods.",
                    icon = Icons.AutoMirrored.Outlined.Rule,
                    checked = uiState.deterministicFirst,
                    onCheckedChange = onDeterministicFirstChange,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleRow(
                    title = "Fallback OCR provider",
                    description = "Allow a cloud OCR service when local extraction is low confidence.",
                    icon = Icons.Outlined.CloudSync,
                    checked = uiState.fallbackOcrProvider,
                    onCheckedChange = onFallbackOcrProviderChange,
                )
            }
        }

        // ── Privacy ──────────────────────────────────────────────────────
        item {
            SettingsGroupCard(title = "Privacy", icon = Icons.Outlined.Security) {
                SettingsToggleRow(
                    title = "Mask sensitive identifiers",
                    description = "Redact ABNs and other identifiers in on-screen summaries.",
                    icon = Icons.Outlined.PrivacyTip,
                    checked = uiState.maskSensitiveIdentifiers,
                    onCheckedChange = onMaskSensitiveIdentifiersChange,
                )
            }
        }

        // ── Innovation ───────────────────────────────────────────────────
        item {
            SettingsGroupCard(title = "Innovation", icon = Icons.Outlined.Lightbulb) {
                SettingsToggleRow(
                    title = "Innovation mode",
                    description = "Enable experimental adaptive extraction features. Disables deterministic-first when active.",
                    icon = Icons.Outlined.Lightbulb,
                    checked = uiState.innovationMode,
                    onCheckedChange = onInnovationModeChange,
                )
            }
        }

        // ── Legal disclaimer ─────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "LedgerOS is for preparation and record management only.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Always review figures with a registered tax agent before lodging with the ATO.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // ── Account ──────────────────────────────────────────────────────
        if (isAuthenticated) {
            item {
                OutlinedButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
            }
        }

        item {
            Text(
                "LedgerOS v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

// ── Subscription cards ────────────────────────────────────────────────────

/** Shown only to the owner account — no pricing, admin shortcut. */
@Composable
private fun OwnerAccessCard(onManageAccessClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "LedgerOS Pro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Owner account — full access, all features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onManageAccessClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Manage user access")
            }
        }
    }
}

/** Shown to users who have been granted complimentary Pro access by the owner. */
@Composable
private fun GrantedProCard(tier: GrantedTier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LedgerOS Pro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Complimentary ${tier.label} access",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                if (tier.originalPriceLabel != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tier.originalPriceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        )
                        Text(
                            "  ·  Waived",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ProActiveCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LedgerOS Pro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "You have full access to all features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ProUpgradeSettingsCard(
    onUpgradeClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upgrade to Pro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProFeatureRow("Unlimited receipts")
                ProFeatureRow("All financial years")
                ProFeatureRow("BAS due-date notifications")
                ProFeatureRow("Multi-business (coming soon)")
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onUpgradeClick, modifier = Modifier.fillMaxWidth()) {
                Text("Go Pro · ${PremiumTier.PRO_MONTHLY_PRICE}/month")
            }
            Text(
                "or ${PremiumTier.PRO_YEARLY_PRICE}/year · ${PremiumTier.PRO_YEARLY_SAVING}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = onRestoreClick, modifier = Modifier.fillMaxWidth()) {
                Text("Restore purchases", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ProFeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(6.dp),
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("  $title", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
