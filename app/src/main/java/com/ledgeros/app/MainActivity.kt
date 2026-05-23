package com.ledgeros.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ledgeros.app.billing.BillingManager
import com.ledgeros.app.ui.LedgerOsApp
import com.ledgeros.app.ui.LedgerViewModel
import com.ledgeros.app.ui.theme.LedgerOsTheme
import com.ledgeros.app.worker.BasNotificationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // The ViewModel is owned by this Activity's ViewModelStore.
    // The same instance is returned when Compose calls viewModel() inside LedgerOsApp.
    private val vm: LedgerViewModel by viewModels { LedgerViewModel.Factory }

    private var billingManager: BillingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wire Play Billing before the first Compose recomposition so the
        // BillingManager reference is available immediately.
        billingManager = BillingManager(this) { vm.onPurchaseAcknowledged() }
        vm.setBillingManager(billingManager!!)

        setContent {
            LedgerOsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LedgerOsApp(vm)
                }
            }
        }

        scheduleBasNotifications()
        requestNotificationPermission()
    }

    override fun onDestroy() {
        billingManager?.endConnection()
        super.onDestroy()
    }

    // ── BAS deadline notifications ────────────────────────────────────────

    private fun scheduleBasNotifications() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BasNotificationWorker.WORK_NAME,
            // KEEP: don't reset the timer if the app restarts
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BasNotificationWorker>(1, TimeUnit.DAYS)
                .build(),
        )
    }

    // ── Runtime notification permission (Android 13+) ─────────────────────

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION,
            )
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }
}
