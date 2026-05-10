package com.batb4016.jarpick.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batb4016.jarpick.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarPickDataStore by preferencesDataStore("jarpick_settings")

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val selectedTheme: String = "Sunlit",
    val freeJarLimit: Int = 3,
    val freeOptionLimitPerJar: Int = 25,
    val freeHistoryLimit: Int = 20,
    val animationEnabled: Boolean = true,
    val shakeToPickEnabled: Boolean = false,
    val lastOpenedLocalDate: String = "",
    val isPremium: Boolean = false,
    val productId: String = "",
    val purchaseTokenHash: String = "",
    val acknowledged: Boolean = false,
    val lastVerifiedAt: Long = 0L,
    val source: String = "local",
)

class UserSettingsStore(private val context: Context) {
    val settings: Flow<UserSettings> = context.jarPickDataStore.data.map { prefs ->
        UserSettings(
            onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
            selectedTheme = prefs[SELECTED_THEME] ?: "Sunlit",
            freeJarLimit = prefs[FREE_JAR_LIMIT] ?: 3,
            freeOptionLimitPerJar = prefs[FREE_OPTION_LIMIT] ?: 25,
            freeHistoryLimit = prefs[FREE_HISTORY_LIMIT] ?: 20,
            animationEnabled = prefs[ANIMATION_ENABLED] ?: true,
            shakeToPickEnabled = prefs[SHAKE_TO_PICK_ENABLED] ?: false,
            lastOpenedLocalDate = prefs[LAST_OPENED_LOCAL_DATE] ?: "",
            isPremium = prefs[IS_PREMIUM] ?: false,
            productId = prefs[PRODUCT_ID] ?: "",
            purchaseTokenHash = prefs[PURCHASE_TOKEN_HASH] ?: "",
            acknowledged = prefs[ACKNOWLEDGED] ?: false,
            lastVerifiedAt = prefs[LAST_VERIFIED_AT] ?: 0L,
            source = prefs[SOURCE] ?: "local",
        )
    }

    suspend fun completeOnboarding() = context.jarPickDataStore.edit { it[ONBOARDING_COMPLETED] = true }

    suspend fun setPremiumOverride(enabled: Boolean) {
        if (!BuildConfig.ALLOW_DEBUG_PREMIUM_OVERRIDE) return
        context.jarPickDataStore.edit {
            it[IS_PREMIUM] = enabled
            it[SOURCE] = "debug_override"
        }
    }

    suspend fun setPremiumFromBilling(productId: String, tokenHash: String, acknowledged: Boolean) {
        context.jarPickDataStore.edit {
            it[IS_PREMIUM] = true
            it[PRODUCT_ID] = productId
            it[PURCHASE_TOKEN_HASH] = tokenHash
            it[ACKNOWLEDGED] = acknowledged
            it[LAST_VERIFIED_AT] = System.currentTimeMillis()
            it[SOURCE] = "billing"
        }
    }

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboardingCompleted")
        private val SELECTED_THEME = stringPreferencesKey("selectedTheme")
        private val FREE_JAR_LIMIT = intPreferencesKey("freeJarLimit")
        private val FREE_OPTION_LIMIT = intPreferencesKey("freeOptionLimitPerJar")
        private val FREE_HISTORY_LIMIT = intPreferencesKey("freeHistoryLimit")
        private val ANIMATION_ENABLED = booleanPreferencesKey("animationEnabled")
        private val SHAKE_TO_PICK_ENABLED = booleanPreferencesKey("shakeToPickEnabled")
        private val LAST_OPENED_LOCAL_DATE = stringPreferencesKey("lastOpenedLocalDate")
        private val IS_PREMIUM = booleanPreferencesKey("isPremium")
        private val PRODUCT_ID = stringPreferencesKey("productId")
        private val PURCHASE_TOKEN_HASH = stringPreferencesKey("purchaseTokenHash")
        private val ACKNOWLEDGED = booleanPreferencesKey("acknowledged")
        private val LAST_VERIFIED_AT = longPreferencesKey("lastVerifiedAt")
        private val SOURCE = stringPreferencesKey("source")
    }
}
