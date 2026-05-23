package com.ledgeros.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ledgeros.app.data.remote.SupabaseClientProvider
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.model.Receipt
import com.ledgeros.app.ui.screens.AdminScreen
import com.ledgeros.app.ui.screens.AuthScreen
import com.ledgeros.app.ui.screens.BasPeriodDetailScreen
import com.ledgeros.app.ui.screens.ComplianceScreen
import com.ledgeros.app.ui.screens.OnboardingScreen
import com.ledgeros.app.ui.screens.DashboardScreen
import com.ledgeros.app.ui.screens.ReceiptDetailScreen
import com.ledgeros.app.ui.screens.ReceiptScreen
import com.ledgeros.app.ui.screens.ReportsScreen
import com.ledgeros.app.ui.screens.SettingsScreen
import com.ledgeros.app.ui.screens.TransactionDetailScreen
import kotlin.math.abs

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val rootDestinations = listOf(
    NavDestination("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavDestination("receipts", "Receipts", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    NavDestination("compliance", "Tasks", Icons.AutoMirrored.Filled.EventNote, Icons.AutoMirrored.Outlined.EventNote),
    NavDestination("reports", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    NavDestination("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

private fun detailScreenTitle(route: String?): String = when {
    route == null -> ""
    route.startsWith("reports/period") -> "Quarter detail"
    route.startsWith("reports/transaction") -> "Transaction"
    route.startsWith("receipts/detail") -> "Receipt"
    route == "admin" -> "Access Management"
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerOsApp(viewModel: LedgerViewModel = viewModel(factory = LedgerViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity

    // Show auth screen if Supabase is configured and user is not signed in
    if (SupabaseClientProvider.isConfigured && !uiState.auth.isAuthenticated) {
        AuthScreen(
            isLoading = uiState.auth.isLoading,
            error = uiState.auth.error,
            awaitingEmailConfirmation = uiState.auth.awaitingEmailConfirmation,
            confirmationEmail = uiState.auth.confirmationEmail,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onBackToSignIn = viewModel::dismissEmailConfirmation,
        )
        return
    }

    // Show onboarding until the user has completed business setup
    if (!uiState.isOnboardingComplete) {
        OnboardingScreen(
            onComplete = { name, abn, gst, freq, pro ->
                viewModel.completeOnboarding(name, abn, gst, freq, pro)
            },
        )
        return
    }

    val navController = rememberNavController()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentRootRoute = currentRoute?.substringBefore("/")
    val isTopLevel = rootDestinations.any { it.route == currentRootRoute }
    val detailTitle = if (!isTopLevel) detailScreenTitle(currentRoute) else ""

    val receipts = uiState.dashboard.recentReceipts

    Scaffold(
        topBar = {
            if (isTopLevel) {
                // Persistent branded title bar — always visible on all root tabs
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "LedgerOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            } else if (detailTitle.isNotBlank()) {
                // Detail screen — back arrow + page-specific title
                TopAppBar(
                    title = {
                        Text(
                            detailTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    rootDestinations.forEach { dest ->
                        val selected = currentRootRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label,
                                )
                            },
                            label = {
                                Text(
                                    dest.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = {
                fadeIn(tween(160)) + slideInHorizontally(tween(220)) { it / 8 }
            },
            exitTransition = {
                fadeOut(tween(110)) + slideOutHorizontally(tween(160)) { -it / 12 }
            },
            popEnterTransition = {
                fadeIn(tween(160)) + slideInHorizontally(tween(220)) { -it / 8 }
            },
            popExitTransition = {
                fadeOut(tween(110)) + slideOutHorizontally(tween(160)) { it / 12 }
            },
        ) {
            composable("dashboard") {
                DashboardScreen(uiState.dashboard)
            }

            composable("receipts") {
                ReceiptScreen(
                    uiState = uiState.receipts,
                    savedReceiptCount = uiState.dashboard.recentReceipts.size,
                    isPremium = uiState.isPremium,
                    onSampleTextChange = viewModel::updateReceiptSample,
                    onUploadClick = viewModel::markReceiptUploadReady,
                    onImageSelected = viewModel::selectReceiptImage,
                    onRunExtractionClick = viewModel::runReceiptExtraction,
                    onMerchantChange = viewModel::updateReceiptDraftMerchant,
                    onDateChange = viewModel::updateReceiptDraftDate,
                    onTotalChange = viewModel::updateReceiptDraftTotal,
                    onGstChange = viewModel::updateReceiptDraftGst,
                    onCategoryChange = viewModel::updateReceiptDraftCategory,
                    onSaveReviewClick = viewModel::saveReceiptDraft,
                    onUpgradeClick = { activity?.let { viewModel.launchBillingFlow(it) } },
                )
            }

            composable("compliance") {
                ComplianceScreen(
                    uiState = uiState.compliance,
                    onStatusChange = { taskId, status -> viewModel.updateTaskStatus(taskId, status) },
                )
            }

            composable("reports") {
                ReportsScreen(
                    uiState = uiState.reports,
                    isPremium = uiState.isPremium,
                    onBankStatementImported = viewModel::importBankStatement,
                    onManualAdjustmentSaved = viewModel::addManualAdjustment,
                    onBasPeriodClick = { navController.navigate("reports/period/$it") },
                    onTransactionClick = { navController.navigate("reports/transaction/$it") },
                    onPreviousFinancialYearClick = viewModel::selectPreviousFinancialYear,
                    onCurrentFinancialYearClick = viewModel::selectCurrentFinancialYear,
                    onNextFinancialYearClick = viewModel::selectNextFinancialYear,
                    onUpgradeClick = { activity?.let { viewModel.launchBillingFlow(it) } },
                )
            }

            composable(
                route = "reports/period/{periodId}",
                arguments = listOf(navArgument("periodId") { type = NavType.StringType }),
            ) { entry ->
                val periodId = entry.arguments?.getString("periodId").orEmpty()
                val period = uiState.reports.basPeriods.firstOrNull { it.id == periodId }
                if (period != null) {
                    BasPeriodDetailScreen(
                        period = period,
                        transactions = uiState.reports.bankTransactions.filter { it.id in period.transactionIds },
                        receipts = receipts.filter { it.id in period.receiptIds },
                        onTransactionClick = { navController.navigate("reports/transaction/$it") },
                        onReceiptClick = { navController.navigate("receipts/detail/$it") },
                    )
                }
            }

            composable(
                route = "reports/transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) { entry ->
                val transactionId = entry.arguments?.getString("transactionId").orEmpty()
                val transaction = uiState.reports.bankTransactions.firstOrNull { it.id == transactionId }
                if (transaction != null) {
                    TransactionDetailScreen(
                        transaction = transaction,
                        matchedReceipt = receipts.matchedReceiptFor(transaction),
                        suggestedReceipt = receipts.bestSuggestedMatchFor(transaction),
                        onReceiptClick = { navController.navigate("receipts/detail/$it") },
                        onSaveClick = { description, date, amount, category, gst ->
                            viewModel.updateBankTransaction(transaction.id, description, date, amount, category, gst)
                        },
                        onAcceptReceiptMatch = { viewModel.acceptReceiptMatch(transaction.id, it) },
                        onClearReceiptMatch = { viewModel.clearReceiptMatch(transaction.id) },
                        onDeleteClick = {
                            viewModel.deleteTransaction(transaction.id)
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable(
                route = "receipts/detail/{receiptId}",
                arguments = listOf(navArgument("receiptId") { type = NavType.StringType }),
            ) { entry ->
                val receiptId = entry.arguments?.getString("receiptId").orEmpty()
                val receipt = receipts.firstOrNull { it.id == receiptId }
                if (receipt != null) {
                    ReceiptDetailScreen(
                        receipt = receipt,
                        onSaveClick = { merchant, date, total, gst, category ->
                            viewModel.updateReceipt(receipt.id, merchant, date, total, gst, category)
                        },
                        onDeleteClick = {
                            viewModel.deleteReceipt(receipt.id)
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable("settings") {
                SettingsScreen(
                    uiState = uiState.settings,
                    isPremium = uiState.isPremium,
                    isAuthenticated = uiState.auth.isAuthenticated,
                    isOwner = uiState.isOwner,
                    grantedTier = uiState.auth.grantedTier,
                    onDeterministicFirstChange = viewModel::setDeterministicFirst,
                    onMaskSensitiveIdentifiersChange = viewModel::setMaskSensitiveIdentifiers,
                    onFallbackOcrProviderChange = viewModel::setFallbackOcrProvider,
                    onInnovationModeChange = viewModel::setInnovationMode,
                    onUpgradeClick = { activity?.let { viewModel.launchBillingFlow(it) } },
                    onRestorePurchasesClick = viewModel::restorePurchases,
                    onSignOutClick = viewModel::signOut,
                    onManageAccessClick = { navController.navigate("admin") },
                )
            }

            composable("admin") {
                AdminScreen(
                    uiState = uiState.admin,
                    onLoad = viewModel::loadManagedUsers,
                    onAddUser = viewModel::addManagedUser,
                    onUpdateTier = viewModel::updateManagedUserTier,
                    onRemoveUser = viewModel::removeManagedUser,
                )
            }
        }
    }
}

private fun List<Receipt>.matchedReceiptFor(transaction: BankTransaction): Receipt? =
    transaction.matchedReceiptId?.let { id -> firstOrNull { it.id == id } }

private fun List<Receipt>.bestSuggestedMatchFor(transaction: BankTransaction): Receipt? {
    if (transaction.matchedReceiptId != null || !transaction.isExpense) return null
    return filter { receipt ->
        val amtDiff = abs(receipt.total - abs(transaction.amount))
        val dayDiff = abs(java.time.temporal.ChronoUnit.DAYS.between(receipt.receiptDate, transaction.transactionDate))
        amtDiff <= maxOf(2.0, receipt.total * 0.03) && dayDiff <= 5
    }.minByOrNull { abs(it.total - abs(transaction.amount)) }
}
