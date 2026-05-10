package com.batb4016.jarpick.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batb4016.jarpick.BuildConfig
import com.batb4016.jarpick.data.JarPickDatabase
import com.batb4016.jarpick.data.JarRepository
import com.batb4016.jarpick.data.JarWithOptions
import com.batb4016.jarpick.data.OptionEntity
import com.batb4016.jarpick.data.PickHistoryEntity
import com.batb4016.jarpick.data.UserSettings
import com.batb4016.jarpick.data.UserSettingsStore
import com.batb4016.jarpick.domain.DecisionMode
import com.batb4016.jarpick.monetization.PurchaseState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JarPickUiState(
    val settings: UserSettings = UserSettings(),
    val jars: List<JarWithOptions> = emptyList(),
    val selectedJarId: String? = null,
    val selectedOptions: List<OptionEntity> = emptyList(),
    val history: List<PickHistoryEntity> = emptyList(),
    val lastPick: PickHistoryEntity? = null,
    val message: String? = null,
) {
    val selectedJar: JarWithOptions? get() = jars.firstOrNull { it.jar.id == selectedJarId }
    val isPremium: Boolean get() = settings.isPremium
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JarPickViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JarRepository(JarPickDatabase.get(application).dao())
    private val settingsStore = UserSettingsStore(application)
    private val selectedJarId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val lastPick = MutableStateFlow<PickHistoryEntity?>(null)

    val uiState: StateFlow<JarPickUiState> = combine(
        settingsStore.settings,
        repository.observeJars(),
        selectedJarId,
        selectedJarId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeOptions(id) },
        repository.observeHistory(),
        lastPick,
        message,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        JarPickUiState(
            settings = values[0] as UserSettings,
            jars = values[1] as List<JarWithOptions>,
            selectedJarId = values[2] as String?,
            selectedOptions = values[3] as List<OptionEntity>,
            history = values[4] as List<PickHistoryEntity>,
            lastPick = values[5] as PickHistoryEntity?,
            message = values[6] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JarPickUiState())

    fun selectJar(jarId: String?) {
        selectedJarId.value = jarId
    }

    fun completeOnboarding() = viewModelScope.launch {
        settingsStore.completeOnboarding()
    }

    fun createStarterJars() = viewModelScope.launch {
        val state = uiState.value
        repository.createStarterJars(state.jars.size, state.isPremium)
            .onSuccess {
                settingsStore.completeOnboarding()
                selectedJarId.value = uiState.value.jars.firstOrNull()?.jar?.id
            }
            .onFailure { message.value = it.message }
    }

    fun createJar(name: String, icon: String = "jar") = viewModelScope.launch {
        val state = uiState.value
        repository.createJar(name, icon, "Sunlit", state.jars.size, state.isPremium)
            .onSuccess { id -> selectedJarId.value = id }
            .onFailure { message.value = it.message }
    }

    fun createOption(text: String, notes: String? = null, weight: Int = 1) = viewModelScope.launch {
        val jarId = selectedJarId.value ?: return@launch
        val state = uiState.value
        repository.createOption(jarId, text, notes, weight, state.selectedOptions.size, state.isPremium)
            .onFailure { message.value = it.message }
    }

    fun setMode(mode: DecisionMode) = viewModelScope.launch {
        val jarId = selectedJarId.value ?: return@launch
        repository.updateJarMode(jarId, mode, uiState.value.isPremium)
            .onFailure { message.value = it.message }
    }

    fun pickSelectedJar(onPicked: () -> Unit) = viewModelScope.launch {
        val jarId = selectedJarId.value ?: return@launch
        val picked = repository.pick(jarId, uiState.value.isPremium, uiState.value.settings.freeHistoryLimit)
        if (picked == null) {
            message.value = "Add at least one available choice before picking."
        } else {
            lastPick.value = picked
            onPicked()
        }
    }

    fun hideLastForNow() = viewModelScope.launch {
        lastPick.value?.optionId?.let { repository.hideOptionForNow(it) }
    }

    fun markLastUsed() = viewModelScope.launch {
        lastPick.value?.optionId?.let { repository.archiveOption(it) }
    }

    fun resetEliminations() = viewModelScope.launch {
        selectedJarId.value?.let { repository.resetEliminations(it) }
    }

    fun deleteAllLocalData() = viewModelScope.launch {
        repository.deleteAllLocalData()
        selectedJarId.value = null
        lastPick.value = null
    }

    fun toggleDebugPremium() = viewModelScope.launch {
        if (BuildConfig.ALLOW_DEBUG_PREMIUM_OVERRIDE) {
            settingsStore.setPremiumOverride(!uiState.value.isPremium)
        }
    }

    fun storeBillingPurchase(purchaseState: PurchaseState) = viewModelScope.launch {
        if (purchaseState.isPremium) {
            settingsStore.setPremiumFromBilling(
                productId = purchaseState.productId.ifBlank { BuildConfig.PREMIUM_PRODUCT_ID },
                tokenHash = purchaseState.purchaseTokenHash,
                acknowledged = purchaseState.acknowledged,
            )
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
