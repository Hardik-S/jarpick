package com.batb4016.jarpick

import android.app.Application
import android.app.Activity
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batb4016.jarpick.domain.DecisionMode
import com.batb4016.jarpick.monetization.JarPickAdSurface
import com.batb4016.jarpick.monetization.JarPickBannerAd
import com.batb4016.jarpick.monetization.BillingManager
import com.batb4016.jarpick.monetization.BillingState
import com.batb4016.jarpick.ui.JarPickUiState
import com.batb4016.jarpick.ui.JarPickViewModel
import kotlinx.coroutines.delay

private enum class Screen {
    Onboarding,
    JarList,
    EditJar,
    JarDetail,
    EditOption,
    PickAnimation,
    Result,
    History,
    Premium,
    Settings,
}

@Composable
fun JarPickApp() {
    val application = LocalContext.current.applicationContext as Application
    val context = LocalContext.current
    val billingManager = remember { BillingManager(context) }
    val billingState by billingManager.billingState.collectAsState()
    val billingPurchaseState by billingManager.purchaseState.collectAsState()
    val viewModel: JarPickViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return JarPickViewModel(application) as T
        }
    })
    val state by viewModel.uiState.collectAsState()
    var screen by remember { mutableStateOf(if (state.settings.onboardingCompleted) Screen.JarList else Screen.Onboarding) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.settings.onboardingCompleted) {
        if (state.settings.onboardingCompleted && screen == Screen.Onboarding) screen = Screen.JarList
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(billingPurchaseState) {
        if (billingPurchaseState.isPremium) viewModel.storeBillingPurchase(billingPurchaseState)
    }
    DisposableEffect(Unit) {
        billingManager.startConnection()
        onDispose { billingManager.endConnection() }
    }

    MaterialTheme(colorScheme = calmJarColorScheme()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.Onboarding -> OnboardingScreen(
                        onCreate = { viewModel.completeOnboarding(); screen = Screen.EditJar },
                        onStarter = { viewModel.createStarterJars(); screen = Screen.JarList },
                    )
                    Screen.JarList -> JarListScreen(
                        state = state,
                        onNewJar = { screen = Screen.EditJar },
                        onOpenJar = { viewModel.selectJar(it); screen = Screen.JarDetail },
                        onHistory = { screen = Screen.History },
                        onPremium = { screen = Screen.Premium },
                        onSettings = { screen = Screen.Settings },
                    )
                    Screen.EditJar -> EditJarScreen(
                        onSave = { name -> viewModel.createJar(name); screen = Screen.JarDetail },
                        onBack = { screen = Screen.JarList },
                    )
                    Screen.JarDetail -> JarDetailScreen(
                        state = state,
                        onBack = { screen = Screen.JarList },
                        onPick = { screen = Screen.PickAnimation },
                        onAddOption = { screen = Screen.EditOption },
                        onMode = viewModel::setMode,
                        onPremium = { screen = Screen.Premium },
                        onReset = viewModel::resetEliminations,
                    )
                    Screen.EditOption -> EditOptionScreen(
                        onSave = { text, weight -> viewModel.createOption(text, weight = weight); screen = Screen.JarDetail },
                        onBack = { screen = Screen.JarDetail },
                    )
                    Screen.PickAnimation -> PickAnimationScreen(
                        onDone = { viewModel.pickSelectedJar { screen = Screen.Result } },
                        onSkip = { viewModel.pickSelectedJar { screen = Screen.Result } },
                    )
                    Screen.Result -> ResultScreen(
                        state = state,
                        onAccept = { screen = Screen.JarDetail },
                        onPickAgain = { screen = Screen.PickAnimation },
                        onHide = { viewModel.hideLastForNow(); screen = Screen.JarDetail },
                        onUsed = { viewModel.markLastUsed(); screen = Screen.JarDetail },
                    )
                    Screen.History -> HistoryScreen(state, onBack = { screen = Screen.JarList })
                    Screen.Premium -> PremiumScreen(
                        state = state,
                        billingState = billingState,
                        onBack = { screen = Screen.JarList },
                        onPurchase = { (context as? Activity)?.let(billingManager::launchRemoveAdsPurchase) },
                        onRestore = billingManager::restorePurchases,
                        onDebugPremium = viewModel::toggleDebugPremium,
                    )
                    Screen.Settings -> SettingsScreen(
                        state = state,
                        onBack = { screen = Screen.JarList },
                        onDeleteAll = viewModel::deleteAllLocalData,
                        onPremium = { screen = Screen.Premium },
                        onRestore = billingManager::restorePurchases,
                        onDebugPremium = viewModel::toggleDebugPremium,
                    )
                }
            }
        }
    }
}

@Composable
private fun calmJarColorScheme() = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF2F6F73),
    secondary = Color(0xFFF2B84B),
    tertiary = Color(0xFF805B9B),
    background = Color(0xFFFFFBF4),
    surface = Color(0xFFFFFBFE),
)

@Composable
private fun OnboardingScreen(onCreate: () -> Unit, onStarter: () -> Unit) {
    CenterColumn {
        Text("Stop overthinking small choices.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Create jars for dinner, chores, movies, games, or anything else. Tap to pick.", textAlign = TextAlign.Center)
        BigButton("Create my first jar", onCreate)
        OutlinedButton(onClick = onStarter) { Text("Use starter jars") }
    }
}

@Composable
private fun JarListScreen(state: JarPickUiState, onNewJar: () -> Unit, onOpenJar: (String) -> Unit, onHistory: () -> Unit, onPremium: () -> Unit, onSettings: () -> Unit) {
    AppScaffold("JarPick", actions = {
        TextButton(onClick = onHistory) { Text("History") }
        TextButton(onClick = onPremium) { Text("Premium") }
        TextButton(onClick = onSettings) { Text("Settings") }
    }, bottom = { JarPickBannerAd(state.isPremium, JarPickAdSurface.JarList, Modifier.fillMaxWidth()) }) {
        if (state.jars.isEmpty()) EmptyState("Create a jar for a decision you repeat.", "New Jar", onNewJar)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { BigButton("New Jar", onNewJar) }
            items(state.jars) { jarWithOptions ->
                Card(Modifier.fillMaxWidth().clickable { onOpenJar(jarWithOptions.jar.id) }, shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${jarIcon(jarWithOptions.jar.icon)} ${jarWithOptions.jar.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${jarWithOptions.options.count { it.archivedAt == null }} choices • ${jarWithOptions.jar.mode}")
                        Text("Last pick: ${jarWithOptions.options.firstOrNull { it.id == jarWithOptions.jar.lastPickedOptionId }?.text ?: "None yet"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditJarScreen(onSave: (String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AppScaffold("New Jar", onBack = onBack) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Jar name") }, modifier = Modifier.fillMaxWidth())
        BigButton("Save jar", onClick = { onSave(name) })
    }
}

@Composable
private fun JarDetailScreen(state: JarPickUiState, onBack: () -> Unit, onPick: () -> Unit, onAddOption: () -> Unit, onMode: (DecisionMode) -> Unit, onPremium: () -> Unit, onReset: () -> Unit) {
    val jar = state.selectedJar
    AppScaffold(jar?.jar?.name ?: "Jar", onBack = onBack, bottom = { JarPickBannerAd(state.isPremium, JarPickAdSurface.JarDetail, Modifier.fillMaxWidth()) }) {
        if (jar == null) EmptyState("Pick a jar to continue.", "Back to jars", onBack) else {
            val activeCount = state.selectedOptions.count { it.archivedAt == null }
            Text("$activeCount choices")
            if (!state.isPremium && activeCount >= 20) Text("Free jars can hold 25 choices. Upgrade for unlimited choices.", color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecisionMode.entries.forEach { mode ->
                    FilterChip(selected = jar.jar.mode == mode.name, onClick = { if (state.isPremium || mode == DecisionMode.FAIR) onMode(mode) else onPremium() }, label = { Text(mode.name.replace('_', ' ')) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("Pick from Jar", onPick, Modifier.weight(1f))
                OutlinedButton(onClick = onAddOption, modifier = Modifier.weight(1f).height(56.dp)) { Text("Add option") }
            }
            OutlinedButton(onClick = onReset) { Text("Reset eliminated options") }
            if (state.selectedOptions.isEmpty()) EmptyState("Add a few choices, then let the jar pick.", "Add option", onAddOption)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.selectedOptions) { option ->
                    Card(shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(option.text, fontWeight = FontWeight.SemiBold)
                            Text("x${option.weight}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditOptionScreen(onSave: (String, Int) -> Unit, onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(1) }
    AppScaffold("Add Choice", onBack = onBack) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Choice") }, modifier = Modifier.fillMaxWidth())
        Text("Weight")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { value -> FilterChip(selected = weight == value, onClick = { weight = value }, label = { Text(value.toString()) }) }
        }
        BigButton("Save choice", onClick = { onSave(text, weight) })
    }
}

@Composable
private fun PickAnimationScreen(onDone: () -> Unit, onSkip: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (started) 8f else -8f, animationSpec = tween(850, easing = LinearOutSlowInEasing), label = "jar shake")
    LaunchedEffect(Unit) {
        started = true
        delay(850)
        onDone()
    }
    CenterColumn(Modifier.clickable { onSkip() }) {
        Text("Shaking the jar...", style = MaterialTheme.typography.titleLarge)
        Text("🏺", style = MaterialTheme.typography.displayLarge, modifier = Modifier.rotate(rotation))
        Text("Tap to skip")
    }
}

@Composable
private fun ResultScreen(state: JarPickUiState, onAccept: () -> Unit, onPickAgain: () -> Unit, onHide: () -> Unit, onUsed: () -> Unit) {
    CenterColumn {
        Text("Your pick:", style = MaterialTheme.typography.titleLarge)
        Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(state.lastPick?.optionTextSnapshot ?: "No pick yet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(28.dp))
        }
        BigButton("Accept", onAccept)
        OutlinedButton(onClick = onPickAgain) { Text("Pick again") }
        OutlinedButton(onClick = onHide) { Text("Hide For Now") }
        TextButton(onClick = onUsed) { Text("Mark Used") }
    }
}

@Composable
private fun HistoryScreen(state: JarPickUiState, onBack: () -> Unit) {
    AppScaffold("History", onBack = onBack) {
        val history = if (state.isPremium) state.history else state.history.take(20)
        if (history.isEmpty()) EmptyState("Your picks will appear here.", "Back to jars", onBack)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) {
                Card(shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(it.optionTextSnapshot, fontWeight = FontWeight.Bold)
                        Text("${it.jarNameSnapshot} • ${it.localDate} • ${it.mode}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumScreen(
    state: JarPickUiState,
    billingState: BillingState,
    onBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDebugPremium: () -> Unit,
) {
    AppScaffold("Upgrade JarPick", onBack = onBack) {
        Text("Upgrade JarPick", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Remove ads forever and unlock unlimited jars, weighted picks, no-repeat mode, and premium themes.")
        listOf("No ads", "Unlimited jars", "Unlimited choices", "Weighted picks", "No-repeat and elimination modes", "Full history").forEach { Text("• $it") }
        val price = (billingState as? BillingState.Ready)?.productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$2.99"
        BigButton(if (state.isPremium) "Premium active" else "Upgrade - $price", onClick = onPurchase)
        OutlinedButton(onClick = onRestore) { Text("Restore Purchase") }
        if (billingState is BillingState.Unavailable) Text("Premium is temporarily unavailable.")
        if (BuildConfig.ALLOW_DEBUG_PREMIUM_OVERRIDE) TextButton(onClick = onDebugPremium) { Text("Debug premium override") }
    }
}

@Composable
private fun SettingsScreen(state: JarPickUiState, onBack: () -> Unit, onDeleteAll: () -> Unit, onPremium: () -> Unit, onRestore: () -> Unit, onDebugPremium: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    AppScaffold("Settings", onBack = onBack) {
        Text("Premium status: ${if (state.isPremium) "Active" else "Free"}")
        OutlinedButton(onClick = onRestore) { Text("Restore purchase") }
        OutlinedButton(onClick = onPremium) { Text("Upgrade JarPick") }
        Text("JarPick stores your jars, choices, and pick history on this device. JarPick does not require an account. If ads are enabled, Google Mobile Ads may collect data for advertising, analytics, and fraud prevention. Premium removes ads.")
        Text("App version ${BuildConfig.VERSION_NAME}")
        if (BuildConfig.ALLOW_DEBUG_PREMIUM_OVERRIDE) TextButton(onClick = onDebugPremium) { Text("Debug premium override") }
        TextButton(onClick = { confirmDelete = true }) { Text("Delete all local data") }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteAll() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            title = { Text("Delete all local data?") },
            text = { Text("This removes jars, choices, and pick history from this device.") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}, bottom: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { if (onBack != null) TextButton(onClick = onBack) { Text("Back") } },
                actions = { actions() },
            )
        },
        bottomBar = bottom,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun CenterColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
}

@Composable
private fun BigButton(label: String, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(56.dp)) { Text(label) }
}

@Composable
private fun EmptyState(message: String, cta: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onClick) { Text(cta) }
        }
    }
}

private fun jarIcon(icon: String): String = when (icon) {
    "dinner" -> "🍽"
    "task" -> "✓"
    "walk" -> "☀"
    "study" -> "✎"
    "game" -> "🎲"
    else -> "◌"
}
