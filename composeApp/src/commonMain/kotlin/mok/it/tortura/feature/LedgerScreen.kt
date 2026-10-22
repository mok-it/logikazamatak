package mok.it.tortura.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import mok.it.tortura.ui.components.*
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun LedgerScreen(
    uiState: LedgerUiState,
    onLoad: () -> Unit,
    onSelectTeam: (Long) -> Unit,
    onUndo: (LedgerEntry) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var pendingUndo by remember { mutableStateOf<LedgerEntry?>(null) }
    LaunchedEffect(Unit) { onLoad() }
    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader("Csapatnapló", "A visszavonás új tranzakciót rögzít; az eredeti bejegyzés megmarad.") {
            AppButton("Vissza", onBack, style = AppButtonStyle.Ghost, leadingIcon = { NavigateBackIcon() })
        }
        uiState.errorMessage?.let { StatusBanner(it, BannerTone.Error, onDismiss = onClearMessages) }
        uiState.message?.let { StatusBanner(it, BannerTone.Success, onDismiss = onClearMessages) }
        FormSection("Csapat") {
            AppSelectField(
                value = uiState.teams.firstOrNull { it.id == uiState.selectedTeamId }?.name.orEmpty(),
                label = "Csapat",
                expanded = expanded,
                options = uiState.teams.mapNotNull { team ->
                    team.id?.let {
                        AppSelectOption(
                            it.toString(),
                            team.name ?: "#$it",
                        )
                    }
                },
                onExpandedChange = { expanded = it },
                onSelect = {
                    pendingUndo = null
                    onSelectTeam(it.value.toLong())
                },
                enabled = !uiState.busy,
            )
            AppButton("Frissítés", onLoad, style = AppButtonStyle.Secondary, loading = uiState.busy, leadingIcon = {
                RefreshIcon()
            })
        }
        if (uiState.entries.isEmpty() && !uiState.busy) {
            EmptyState("Nincs tranzakció", "A csapat feladatpróbái, gyógyításai és vásárlásai itt jelennek meg.")
        }
        pendingUndo?.let { entry ->
            FormSection("Visszavonás megerősítése", description = entry.title) {
                Text(
                    "A kapcsolódó bónuszok és gyógyítások is visszavonódnak. Függő vásárlások vagy feloldások esetén a művelet elutasítható.",
                )
                InlineActionRow {
                    AppButton("Mégse", { pendingUndo = null }, style = AppButtonStyle.Ghost, enabled = !uiState.busy)
                    AppButton("Visszavonás", {
                        pendingUndo = null
                        onUndo(entry)
                    }, style = AppButtonStyle.Danger, enabled = !uiState.busy)
                }
            }
        }
        uiState.entries.forEach { entry ->
            SectionCard(toned = entry.reversesId != null || entry.reversed) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "#${entry.id} • ${entry.createdAt?.toString() ?: "Ismeretlen időpont"} • Kezelő: ${entry.userId?.toString() ?: "nincs rögzítve"}",
                )
                when {
                    entry.reversesId != null -> Text(
                        "Visszavonó tranzakció: #${entry.reversesId}",
                        color = AppThemeTokens.colors.textSecondary,
                    )

                    entry.reversed -> Text("Visszavonva", color = AppThemeTokens.colors.textSecondary)

                    entry.automatic -> Text("Automatikus bónusz: az eredeti próbával vagy vásárlással vonható vissza.")

                    else -> AppButton("Visszavonás", {
                        pendingUndo = entry
                    }, style = AppButtonStyle.Secondary, enabled = !uiState.busy)
                }
            }
        }
    }
}
