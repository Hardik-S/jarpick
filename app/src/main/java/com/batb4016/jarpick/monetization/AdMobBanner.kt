package com.batb4016.jarpick.monetization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.batb4016.jarpick.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Single banner-ad gate for JarPick.
 *
 * Decisions captured here:
 * - Debug builds always use Google's public test banner ID so local work cannot
 *   accidentally generate production ad traffic.
 * - Release builds read the production ad unit from BuildConfig via reflection
 *   and accept both the current skeleton field name and the generic fallback
 *   name documented for future Gradle cleanup.
 * - Callers pass the premium entitlement state and screen surface explicitly.
 *   The wrapper refuses to render when the user owns remove_ads_premium or when
 *   the current surface is not approved for ads.
 */
object AdMobBannerConfig {
    const val DEBUG_TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    private val productionFields = listOf(
        "ADMOB_PRODUCTION_BANNER_ID",
        "ADMOB_BANNER_AD_UNIT_ID",
    )

    fun bannerAdUnitId(): String? {
        if (BuildConfig.DEBUG) return DEBUG_TEST_BANNER_AD_UNIT_ID

        return productionFields.firstNotNullOfOrNull { fieldName ->
            runCatching {
                BuildConfig::class.java.getField(fieldName).get(null) as? String
            }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}

enum class JarPickAdSurface(val allowsBanner: Boolean) {
    JarList(allowsBanner = true),
    JarDetail(allowsBanner = true),
    PickerResult(allowsBanner = false),
    CreateOrEditJar(allowsBanner = false),
    Settings(allowsBanner = false),
    PremiumPurchase(allowsBanner = false),
}

fun shouldShowJarPickBanner(
    isPremium: Boolean,
    surface: JarPickAdSurface,
    adUnitId: String? = AdMobBannerConfig.bannerAdUnitId(),
): Boolean = !isPremium && surface.allowsBanner && !adUnitId.isNullOrBlank()

fun createJarPickBannerAdView(
    context: Context,
    adUnitId: String,
): AdView = AdView(context).apply {
    setAdSize(AdSize.BANNER)
    this.adUnitId = adUnitId
    loadAd(AdRequest.Builder().build())
}

@Composable
fun JarPickBannerAd(
    isPremium: Boolean,
    surface: JarPickAdSurface,
    modifier: Modifier = Modifier,
) {
    val adUnitId = AdMobBannerConfig.bannerAdUnitId()
    if (!shouldShowJarPickBanner(isPremium = isPremium, surface = surface, adUnitId = adUnitId)) {
        return
    }
    val resolvedAdUnitId = adUnitId ?: return

    AndroidView(
        modifier = modifier,
        factory = { context -> createJarPickBannerAdView(context, adUnitId = resolvedAdUnitId) },
        update = { adView ->
            if (adView.adUnitId != resolvedAdUnitId) {
                adView.adUnitId = resolvedAdUnitId
                adView.loadAd(AdRequest.Builder().build())
            }
        },
    )
}
