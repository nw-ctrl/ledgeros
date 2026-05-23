package com.ledgeros.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.ledgeros.app.ui.state.PremiumTier

/**
 * Wraps Google Play Billing Library 7.x.
 *
 * ── Prerequisites before purchases will work ──────────────────────────────
 *   1. Publish the app to an internal test track in Play Console.
 *   2. Create subscription products with IDs matching:
 *        • [PremiumTier.PRO_MONTHLY_SKU]  →  "ledgeros_pro_monthly"
 *        • [PremiumTier.PRO_YEARLY_SKU]   →  "ledgeros_pro_yearly"
 *   3. Add tester email addresses under Play Console → License testing.
 *
 * Until the products are live, [launchPurchaseFlow] silently returns without
 * showing a dialog. The dev-only local-grant stub in [LedgerViewModel] keeps
 * the upgrade UX testable in debug builds while the Play Console is being set up.
 */
class BillingManager(
    context: Context,
    private val onPremiumGranted: () -> Unit,
) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    init { connect() }

    // ── Connection ────────────────────────────────────────────────────────

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Silently restore any active subscription on every connect
                    checkExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reconnect lazily on next user action — no polling loop
            }
        })
    }

    // ── Purchase flow ─────────────────────────────────────────────────────

    /**
     * Query Play Store for [sku] product details and launch the purchase sheet.
     * Call this from the UI layer (pass the hosting [Activity]).
     */
    fun launchPurchaseFlow(
        activity: Activity,
        sku: String = PremiumTier.PRO_MONTHLY_SKU,
    ) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(productList).build(),
        ) { result, products ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || products.isEmpty()) return@queryProductDetailsAsync
            val product = products.first()
            val offerToken = product.subscriptionOfferDetails
                ?.firstOrNull()?.offerToken ?: return@queryProductDetailsAsync
            billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(product)
                                .setOfferToken(offerToken)
                                .build(),
                        ),
                    ).build(),
            )
        }
    }

    // ── Purchase result callback ──────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledge(purchase)
                }
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            onPremiumGranted()
            return
        }
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build(),
        ) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                onPremiumGranted()
            }
        }
    }

    // ── Restore purchases ─────────────────────────────────────────────────

    /** Checks Play Store for any existing subscription and grants premium if found. */
    fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach { onPremiumGranted() }
            }
        }
    }

    fun endConnection() = billingClient.endConnection()
}
