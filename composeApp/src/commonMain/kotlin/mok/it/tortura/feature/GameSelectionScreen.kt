package mok.it.tortura.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Game
import mok.it.tortura.model.ItemEffect
import mok.it.tortura.ui.components.AddIcon
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.AppNumberField
import mok.it.tortura.ui.components.AppSelectField
import mok.it.tortura.ui.components.AppSelectOption
import mok.it.tortura.ui.components.AppTextField
import mok.it.tortura.ui.components.BannerTone
import mok.it.tortura.ui.components.DeleteIcon
import mok.it.tortura.ui.components.EmptyState
import mok.it.tortura.ui.components.FormSection
import mok.it.tortura.ui.components.InlineActionRow
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.SaveIcon
import mok.it.tortura.ui.components.SectionCard
import mok.it.tortura.ui.components.StatusBanner
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun GameSelectionScreen(
    uiState: GameSelectionUiState = GameSelectionUiState(),
    onLoad: () -> Unit = {},
    onGameNameChange: (String) -> Unit = {},
    onCreateGame: () -> Unit = {},
    onAddLocation: () -> Unit = {},
    onLocationNameChange: (Long, String) -> Unit = { _, _ -> },
    onRemoveLocation: (Long) -> Unit = {},
    onAddTask: (Long) -> Unit = {},
    onTaskTextChange: (Long, String) -> Unit = { _, _ -> },
    onTaskSolutionChange: (Long, String) -> Unit = { _, _ -> },
    onTaskMiniBossChange: (Long, Boolean) -> Unit = { _, _ -> },
    onRemoveTask: (Long) -> Unit = {},
    onAddHealingTask: () -> Unit = {},
    onHealingTaskTextChange: (Long, String) -> Unit = { _, _ -> },
    onHealingTaskSolutionChange: (Long, String) -> Unit = { _, _ -> },
    onRemoveHealingTask: (Long) -> Unit = {},
    onAddShopItem: () -> Unit = {},
    onShopItemNameChange: (Long, String) -> Unit = { _, _ -> },
    onShopItemPriceChange: (Long, String) -> Unit = { _, _ -> },
    onShopItemMaxPerTeamChange: (Long, String) -> Unit = { _, _ -> },
    onShopItemEffectIdChange: (Long, String) -> Unit = { _, _ -> },
    onRemoveShopItem: (Long) -> Unit = {},
    onSelectGame: (Game) -> Unit = {},
    onGameSelected: (Game) -> Unit = {},
    onLocationSelectionRequired: (Long) -> Unit = {},
    onClearMessages: () -> Unit = {},
) {
    val spacing = AppThemeTokens.spacing

    LaunchedEffect(Unit) {
        onLoad()
    }

    LaunchedEffect(uiState.selectedGame) {
        uiState.selectedGame?.let(onGameSelected)
    }

    LaunchedEffect(uiState.locationSelectionGameId) {
        uiState.locationSelectionGameId?.let { gameId ->
            onLocationSelectionRequired(gameId)
        }
    }

    PageScaffold(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        PageHeader(
            title = "Játékok kezelése",
            description = "Hozz létre új játékot strukturált szerkesztővel, vagy csatlakozz egy meglévőhöz.",
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val bannerMessage = uiState.errorMessage ?: uiState.message
        if (bannerMessage != null) {
            StatusBanner(
                message = bannerMessage,
                tone = if (uiState.errorMessage != null) BannerTone.Error else BannerTone.Success,
                onDismiss = onClearMessages,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val splitLayout = maxWidth >= 960.dp

            if (splitLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        CreateGameEditor(
                            uiState = uiState,
                            onGameNameChange = onGameNameChange,
                            onCreateGame = onCreateGame,
                            onAddLocation = onAddLocation,
                            onLocationNameChange = onLocationNameChange,
                            onRemoveLocation = onRemoveLocation,
                            onAddTask = onAddTask,
                            onTaskTextChange = onTaskTextChange,
                            onTaskSolutionChange = onTaskSolutionChange,
                            onTaskMiniBossChange = onTaskMiniBossChange,
                            onRemoveTask = onRemoveTask,
                            onAddHealingTask = onAddHealingTask,
                            onHealingTaskTextChange = onHealingTaskTextChange,
                            onHealingTaskSolutionChange = onHealingTaskSolutionChange,
                            onRemoveHealingTask = onRemoveHealingTask,
                            onAddShopItem = onAddShopItem,
                            onShopItemNameChange = onShopItemNameChange,
                            onShopItemPriceChange = onShopItemPriceChange,
                            onShopItemMaxPerTeamChange = onShopItemMaxPerTeamChange,
                            onShopItemEffectIdChange = onShopItemEffectIdChange,
                            onRemoveShopItem = onRemoveShopItem,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        ExistingGamesSection(
                            games = uiState.games,
                            isLoading = uiState.isLoading,
                            onSelectGame = onSelectGame,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
                    CreateGameEditor(
                        uiState = uiState,
                        onGameNameChange = onGameNameChange,
                        onCreateGame = onCreateGame,
                        onAddLocation = onAddLocation,
                        onLocationNameChange = onLocationNameChange,
                        onRemoveLocation = onRemoveLocation,
                        onAddTask = onAddTask,
                        onTaskTextChange = onTaskTextChange,
                        onTaskSolutionChange = onTaskSolutionChange,
                        onTaskMiniBossChange = onTaskMiniBossChange,
                        onRemoveTask = onRemoveTask,
                        onAddHealingTask = onAddHealingTask,
                        onHealingTaskTextChange = onHealingTaskTextChange,
                        onHealingTaskSolutionChange = onHealingTaskSolutionChange,
                        onRemoveHealingTask = onRemoveHealingTask,
                        onAddShopItem = onAddShopItem,
                        onShopItemNameChange = onShopItemNameChange,
                        onShopItemPriceChange = onShopItemPriceChange,
                        onShopItemMaxPerTeamChange = onShopItemMaxPerTeamChange,
                        onShopItemEffectIdChange = onShopItemEffectIdChange,
                        onRemoveShopItem = onRemoveShopItem,
                    )
                    ExistingGamesSection(
                        games = uiState.games,
                        isLoading = uiState.isLoading,
                        onSelectGame = onSelectGame,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateGameEditor(
    uiState: GameSelectionUiState,
    onGameNameChange: (String) -> Unit,
    onCreateGame: () -> Unit,
    onAddLocation: () -> Unit,
    onLocationNameChange: (Long, String) -> Unit,
    onRemoveLocation: (Long) -> Unit,
    onAddTask: (Long) -> Unit,
    onTaskTextChange: (Long, String) -> Unit,
    onTaskSolutionChange: (Long, String) -> Unit,
    onTaskMiniBossChange: (Long, Boolean) -> Unit,
    onRemoveTask: (Long) -> Unit,
    onAddHealingTask: () -> Unit,
    onHealingTaskTextChange: (Long, String) -> Unit,
    onHealingTaskSolutionChange: (Long, String) -> Unit,
    onRemoveHealingTask: (Long) -> Unit,
    onAddShopItem: () -> Unit,
    onShopItemNameChange: (Long, String) -> Unit,
    onShopItemPriceChange: (Long, String) -> Unit,
    onShopItemMaxPerTeamChange: (Long, String) -> Unit,
    onShopItemEffectIdChange: (Long, String) -> Unit,
    onRemoveShopItem: (Long) -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing
    val validationError = uiState.createGameValidationError()

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
        FormSection(
            title = "Új játék",
            description = "Adj nevet a játéknak, majd töltsd ki a helyszíneket, feladatokat és bolti tárgyakat.",
        ) {
            AppTextField(
                value = uiState.gameName,
                onValueChange = onGameNameChange,
                label = "Játék neve",
                singleLine = true,
                isError = uiState.gameName.trim().isEmpty(),
                supportingText = if (uiState.gameName.trim().isEmpty()) "Kötelező" else null,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        DraftSection(
            title = "Helyszínek és feladatok",
            description = "Minden helyszínhez több feladat is rendelhető.",
            emptyText = "Még nincs helyszín hozzáadva.",
            addText = "Helyszín hozzáadása",
            isEmpty = uiState.draftLocations.isEmpty(),
            enabled = !uiState.isLoading,
            onAdd = onAddLocation,
        ) {
            uiState.draftLocations.forEach { location ->
                LocationDraftCard(
                    location = location,
                    tasks = uiState.draftTasks.filter { it.locationLocalId == location.localId },
                    enabled = !uiState.isLoading,
                    onLocationNameChange = onLocationNameChange,
                    onRemoveLocation = onRemoveLocation,
                    onAddTask = onAddTask,
                    onTaskTextChange = onTaskTextChange,
                    onTaskSolutionChange = onTaskSolutionChange,
                    onTaskMiniBossChange = onTaskMiniBossChange,
                    onRemoveTask = onRemoveTask,
                )
            }
        }

        DraftSection(
            title = "Gyógyító feladatok",
            description = "Külön kezelt, globális feladatok a játékhoz.",
            emptyText = "Még nincs gyógyító feladat hozzáadva.",
            addText = "Gyógyító feladat hozzáadása",
            isEmpty = uiState.draftHealingTasks.isEmpty(),
            enabled = !uiState.isLoading,
            onAdd = onAddHealingTask,
        ) {
            uiState.draftHealingTasks.forEach { healingTask ->
                HealingTaskDraftCard(
                    healingTask = healingTask,
                    enabled = !uiState.isLoading,
                    onTextChange = onHealingTaskTextChange,
                    onSolutionChange = onHealingTaskSolutionChange,
                    onRemove = onRemoveHealingTask,
                )
            }
        }

        DraftSection(
            title = "Bolt",
            description = "A tárgyak árát és csapatlimitet webes űrlapként, szigorú numerikus mezőkkel szerkeszd.",
            emptyText = "Még nincs bolti tárgy hozzáadva.",
            addText = "Bolti tárgy hozzáadása",
            isEmpty = uiState.draftShopItems.isEmpty(),
            enabled = !uiState.isLoading,
            onAdd = onAddShopItem,
        ) {
            uiState.draftShopItems.forEach { item ->
                ShopItemDraftCard(
                    item = item,
                    enabled = !uiState.isLoading,
                    onNameChange = onShopItemNameChange,
                    onPriceChange = onShopItemPriceChange,
                    onMaxPerTeamChange = onShopItemMaxPerTeamChange,
                    onEffectIdChange = onShopItemEffectIdChange,
                    onRemove = onRemoveShopItem,
                    itemEffects = uiState.itemEffects,
                )
            }
        }

        SectionCard {
            if (validationError != null) {
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.danger,
                )
            }
            InlineActionRow {
                AppButton(
                    text = "Mentés és tovább",
                    onClick = onCreateGame,
                    enabled = !uiState.isLoading && validationError == null,
                    leadingIcon = { SaveIcon() },
                )
            }
        }
    }
}

@Composable
private fun ExistingGamesSection(
    games: List<Game>,
    isLoading: Boolean,
    onSelectGame: (Game) -> Unit,
) {
    FormSection(
        title = "Meglévő játékok",
        description = "Válassz egy már mentett játékot a folytatáshoz.",
    ) {
        if (games.isEmpty() && !isLoading) {
            EmptyState(
                title = "Nincs még mentett játék",
                description = "Hozz létre egy újat a bal oldali szerkesztőben, és innen rögtön folytathatod is.",
            )
        } else {
            games.forEach { game ->
                SelectionRow(
                    title = game.name ?: "Névtelen játék",
                    subtitle = "ID ${game.id ?: "-"}",
                    actionLabel = "Csatlakozás",
                    enabled = !isLoading && game.id != null,
                    onAction = { onSelectGame(game) },
                )
            }
        }
    }
}

@Composable
private fun DraftSection(
    title: String,
    description: String,
    emptyText: String,
    addText: String,
    isEmpty: Boolean,
    enabled: Boolean,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing
    var isExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    FormSection(
        title = title,
        description = description,
        headerAction = {
            AppButton(
                text = addText,
                onClick = onAdd,
                enabled = enabled,
                style = AppButtonStyle.Secondary,
                leadingIcon = { AddIcon() },
            )
            IconButton(onClick = { isExpanded = !isExpanded }, enabled = enabled) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Szakasz bezárása" else "Szakasz megnyitása",
                    modifier = Modifier.rotate(arrowRotation),
                    tint = colors.textSecondary,
                )
            }
        },
    ) {
        if (!isExpanded) {
            Text(
                text = "A szakasz össze van csukva.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            return@FormSection
        }

        if (isEmpty) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg), content = content)
        }
    }
}

@Composable
private fun LocationDraftCard(
    location: CreateLocationDraft,
    tasks: List<CreateTaskDraft>,
    enabled: Boolean,
    onLocationNameChange: (Long, String) -> Unit,
    onRemoveLocation: (Long) -> Unit,
    onAddTask: (Long) -> Unit,
    onTaskTextChange: (Long, String) -> Unit,
    onTaskSolutionChange: (Long, String) -> Unit,
    onTaskMiniBossChange: (Long, Boolean) -> Unit,
    onRemoveTask: (Long) -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    SectionCard(toned = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            AppTextField(
                value = location.name,
                onValueChange = { onLocationNameChange(location.localId, it) },
                label = "Helyszín neve",
                singleLine = true,
                isError = location.name.trim().isEmpty(),
                supportingText = if (location.name.trim().isEmpty()) "Kötelező" else null,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = "Törlés",
                onClick = { onRemoveLocation(location.localId) },
                enabled = enabled,
                style = AppButtonStyle.Danger,
                leadingIcon = { DeleteIcon() },
            )
        }

        if (tasks.isEmpty()) {
            Text(
                text = "Ehhez a helyszínhez még nincs feladat.",
                style = MaterialTheme.typography.bodySmall,
                color = AppThemeTokens.colors.textSecondary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                tasks.forEach { task ->
                    TaskDraftCard(
                        task = task,
                        enabled = enabled,
                        onTextChange = onTaskTextChange,
                        onSolutionChange = onTaskSolutionChange,
                        onMiniBossChange = onTaskMiniBossChange,
                        onRemove = onRemoveTask,
                    )
                }
            }
        }

        InlineActionRow {
            AppButton(
                text = "Feladat hozzáadása",
                onClick = { onAddTask(location.localId) },
                enabled = enabled,
                style = AppButtonStyle.Secondary,
                leadingIcon = { AddIcon() },
            )
        }
    }
}

@Composable
private fun TaskDraftCard(
    task: CreateTaskDraft,
    enabled: Boolean,
    onTextChange: (Long, String) -> Unit,
    onSolutionChange: (Long, String) -> Unit,
    onMiniBossChange: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    SectionCard(toned = true) {
        AppTextField(
            value = task.text,
            onValueChange = { onTextChange(task.localId, it) },
            label = "Feladat szövege",
            isError = task.text.trim().isEmpty(),
            supportingText = if (task.text.trim().isEmpty()) "Kötelező" else null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = task.solution,
            onValueChange = { onSolutionChange(task.localId, it) },
            label = "Megoldás",
            singleLine = true,
            isError = task.solution.trim().isEmpty(),
            supportingText = if (task.solution.trim().isEmpty()) "Kötelező" else null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = task.isMiniBoss,
                    onCheckedChange = { onMiniBossChange(task.localId, it) },
                    enabled = enabled,
                )
                Text(
                    text = "Mini boss",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
            AppButton(
                text = "Feladat törlése",
                onClick = { onRemove(task.localId) },
                enabled = enabled,
                style = AppButtonStyle.Ghost,
                leadingIcon = { DeleteIcon() },
            )
        }
    }
}

@Composable
private fun HealingTaskDraftCard(
    healingTask: CreateHealingTaskDraft,
    enabled: Boolean,
    onTextChange: (Long, String) -> Unit,
    onSolutionChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    SectionCard(toned = true) {
        AppTextField(
            value = healingTask.text,
            onValueChange = { onTextChange(healingTask.localId, it) },
            label = "Feladat szövege",
            isError = healingTask.text.trim().isEmpty(),
            supportingText = if (healingTask.text.trim().isEmpty()) "Kötelező" else null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = healingTask.solution,
            onValueChange = { onSolutionChange(healingTask.localId, it) },
            label = "Megoldás",
            singleLine = true,
            isError = healingTask.solution.trim().isEmpty(),
            supportingText = if (healingTask.solution.trim().isEmpty()) "Kötelező" else null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        InlineActionRow {
            AppButton(
                text = "Törlés",
                onClick = { onRemove(healingTask.localId) },
                enabled = enabled,
                style = AppButtonStyle.Ghost,
                leadingIcon = { DeleteIcon() },
            )
        }
    }
}

@Composable
private fun ShopItemDraftCard(
    item: CreateShopItemDraft,
    enabled: Boolean,
    onNameChange: (Long, String) -> Unit,
    onPriceChange: (Long, String) -> Unit,
    onMaxPerTeamChange: (Long, String) -> Unit,
    onEffectIdChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
    itemEffects: List<ItemEffect>,
) {
    val spacing = AppThemeTokens.spacing

    SectionCard(toned = true) {
        AppTextField(
            value = item.name,
            onValueChange = { onNameChange(item.localId, it) },
            label = "Tárgy neve",
            singleLine = true,
            isError = item.name.trim().isEmpty(),
            supportingText = if (item.name.trim().isEmpty()) "Kötelező" else null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        ResponsiveFieldRow(
            first = {
                AppNumberField(
                    value = item.price,
                    onValueChange = { onPriceChange(item.localId, it) },
                    label = "Ár",
                    isError = item.price.toIntOrNull()?.let { value -> value >= 0 } != true,
                    supportingText = if (item.price.toIntOrNull()?.let { value -> value >= 0 } != true) {
                        "Nem negatív egész"
                    } else {
                        null
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            second = {
                AppNumberField(
                    value = item.maxPerTeam,
                    onValueChange = { onMaxPerTeamChange(item.localId, it) },
                    label = "Csapatonként",
                    isError = item.maxPerTeam.toIntOrNull()?.let { value -> value > 0 } != true,
                    supportingText = if (item.maxPerTeam.toIntOrNull()?.let { value -> value > 0 } != true) {
                        "Pozitív egész"
                    } else {
                        null
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
        ItemEffectDropdown(
            selectedItemEffectId = item.itemEffectId,
            itemEffects = itemEffects,
            enabled = enabled,
            onSelect = { onEffectIdChange(item.localId, it) },
            modifier = Modifier.fillMaxWidth(),
        )
        InlineActionRow {
            AppButton(
                text = "Törlés",
                onClick = { onRemove(item.localId) },
                enabled = enabled,
                style = AppButtonStyle.Ghost,
                leadingIcon = { DeleteIcon() },
            )
        }
    }
}

@Composable
private fun ResponsiveFieldRow(
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 680.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), content = first)
                Column(modifier = Modifier.weight(1f), content = second)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                first()
                second()
            }
        }
    }
}

@Composable
private fun ItemEffectDropdown(
    selectedItemEffectId: String,
    itemEffects: List<ItemEffect>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedEffect = itemEffects.firstOrNull { it.id?.toString() == selectedItemEffectId }
    val label = selectedEffect?.let { "${it.id ?: "-"}: ${it.description.orEmpty()}" } ?: "Nincs effekt"
    val options = buildList {
        add(AppSelectOption(value = "", label = "Nincs effekt"))
        itemEffects.forEach { effect ->
            add(
                AppSelectOption(
                    value = effect.id?.toString().orEmpty(),
                    label = "${effect.id ?: "-"}: ${effect.description.orEmpty()}",
                    enabled = effect.id != null,
                ),
            )
        }
    }

    AppSelectField(
        value = label,
        label = "Effekt",
        expanded = isExpanded,
        options = options,
        onExpandedChange = { isExpanded = it },
        onSelect = { option -> onSelect(option.value) },
        modifier = modifier.widthIn(max = 520.dp),
        enabled = enabled,
    )
}
