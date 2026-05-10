package com.batb4016.jarpick.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Billing Library 8.3.0 wrapper for JarPick's one-time ad-removal product.
 *
 * The app does not have a backend in the MVP. This manager therefore exposes a
 * conservative client-side entitlement only after Play reports a PURCHASED
 * remove_ads_premium purchase and the purchase has been queued for
 * acknowledgement. A production backend can later replace the entitlement
 * decision behind the same StateFlow surface.
 */
class BillingManager(
    context: Context,
    private val externalScope: CoroutineScope? = null,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Unavailable("Billing not connected yet."))
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _purchaseState = MutableStateFlow(PurchaseState())
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private var removeAdsProductDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enableAutoServiceReconnection()
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            refreshCatalogAndPurchases()
            return
        }

        _billingState.value = BillingState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.isOk()) {
                    refreshCatalogAndPurchases()
                } else {
                    markUnavailable("Billing setup failed: ${billingResult.debugMessage.orCode(billingResult)}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.Unavailable("Billing service disconnected; retrying when Play reconnects.")
            }
        })
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun launchRemoveAdsPurchase(activity: Activity): BillingResult {
        val details = removeAdsProductDetails
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("remove_ads_premium product details are unavailable.")
                .build()

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        details.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken
            ?.takeIf { it.isNotBlank() }
            ?.let(productDetailsParamsBuilder::setOfferToken)

        val productDetailsParams = productDetailsParamsBuilder.build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        _purchaseState.value = _purchaseState.value.copy(isPurchaseInFlight = true, lastError = null)

        return billingClient.launchBillingFlow(activity, billingFlowParams).also { result ->
            if (!result.isOk()) {
                _purchaseState.value = _purchaseState.value.copy(
                    isPurchaseInFlight = false,
                    lastError = result.debugMessage.orCode(result),
                )
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        queryOwnedPurchases()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = _purchaseState.value.copy(isPurchaseInFlight = false)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> {
                _purchaseState.value = _purchaseState.value.copy(
                    isPurchaseInFlight = false,
                    lastError = billingResult.debugMessage.orCode(billingResult),
                )
            }
        }
    }

    private fun refreshCatalogAndPurchases() {
        _billingState.value = BillingState.Ready(productDetails = null)
        queryProductDetails()
        queryOwnedPurchases()
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(REMOVE_ADS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (!billingResult.isOk()) {
                markUnavailable("Product lookup failed: ${billingResult.debugMessage.orCode(billingResult)}")
                return@queryProductDetailsAsync
            }

            removeAdsProductDetails = productDetailsResult.productDetailsList
                .firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID }

            val unfetched = productDetailsResult.unfetchedProductList.joinToString { it.productId }
            _billingState.value = if (removeAdsProductDetails == null) {
                BillingState.Unavailable(
                    if (unfetched.isBlank()) {
                        "remove_ads_premium is not configured or not available for this Play account."
                    } else {
                        "remove_ads_premium was not fetched from Play: $unfetched."
                    },
                )
            } else {
                BillingState.Ready(productDetails = removeAdsProductDetails)
            }
        }
    }

    private fun queryOwnedPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.isOk()) {
                processPurchases(purchases)
            } else {
                _purchaseState.value = _purchaseState.value.copy(
                    lastError = billingResult.debugMessage.orCode(billingResult),
                )
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val removeAdsPurchase = purchases.firstOrNull { purchase ->
            REMOVE_ADS_PRODUCT_ID in purchase.products
        }

        if (removeAdsPurchase == null) {
            _purchaseState.value = PurchaseState(isPremium = false)
            return
        }

        when (removeAdsPurchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                _purchaseState.value = PurchaseState(
                    isPremium = true,
                    productId = REMOVE_ADS_PRODUCT_ID,
                    purchaseTokenHash = purchaseTokenHash(removeAdsPurchase.purchaseToken),
                    acknowledged = removeAdsPurchase.isAcknowledged,
                    isPurchaseInFlight = false,
                )
                acknowledgeIfNeeded(removeAdsPurchase)
            }
            Purchase.PurchaseState.PENDING -> {
                _purchaseState.value = PurchaseState(
                    isPremium = false,
                    isPending = true,
                    isPurchaseInFlight = false,
                    lastError = "Purchase is pending; premium unlock waits for Play confirmation.",
                )
            }
            else -> {
                _purchaseState.value = PurchaseState(isPremium = false)
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        scope.launch {
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (!billingResult.isOk()) {
                    _purchaseState.value = _purchaseState.value.copy(
                        lastError = "Purchase acknowledgement failed: ${billingResult.debugMessage.orCode(billingResult)}",
                    )
                }
            }
        }
    }

    private fun markUnavailable(message: String) {
        removeAdsProductDetails = null
        _billingState.value = BillingState.Unavailable(message)
    }

    private fun purchaseTokenHash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun BillingResult.isOk(): Boolean =
        responseCode == BillingClient.BillingResponseCode.OK

    private fun String?.orCode(result: BillingResult): String =
        this?.takeIf { it.isNotBlank() } ?: "Billing response ${result.responseCode}"

    companion object {
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads_premium"
    }
}

sealed interface BillingState {
    data object Connecting : BillingState
    data class Ready(val productDetails: ProductDetails?) : BillingState
    data class Unavailable(val reason: String) : BillingState
}

data class PurchaseState(
    val isPremium: Boolean = false,
    val productId: String = "",
    val purchaseTokenHash: String = "",
    val acknowledged: Boolean = false,
    val isPending: Boolean = false,
    val isPurchaseInFlight: Boolean = false,
    val lastError: String? = null,
)
