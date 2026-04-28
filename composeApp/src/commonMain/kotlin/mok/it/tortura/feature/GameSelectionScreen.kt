package mok.it.tortura.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Game
import mok.it.tortura.model.ItemEffect

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
    onClearMessages: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    LaunchedEffect(uiState.selectedGame) {
        uiState.selectedGame?.let(onGameSelected)
    }

    Scaffold(
        snackbarHost = {
            if (uiState.errorMessage != null || uiState.message != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onClearMessages) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(uiState.errorMessage ?: uiState.message.orEmpty())
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Új játék létrehozása", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Adj nevet a játéknak, majd szükség szerint add hozzá a helyszíneket, feladatokat és bolti tárgyakat.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

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

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Meglévő játékok", style = MaterialTheme.typography.headlineSmall)
                if (uiState.games.isEmpty() && !uiState.isLoading) {
                    Text("Nincs még mentett játék", style = MaterialTheme.typography.bodyMedium)
                }
                uiState.games.forEach { game ->
                    GameRow(
                        game = game,
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
    val validationError = uiState.createGameValidationError()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = uiState.gameName,
            onValueChange = onGameNameChange,
            label = { Text("Játék neve") },
            singleLine = true,
            isError = uiState.gameName.trim().isEmpty(),
            supportingText = {
                if (uiState.gameName.trim().isEmpty()) {
                    Text("Kötelező")
                }
            },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        DraftSection(
            title = "Helyszínek és feladatok",
            emptyText = "Még nincs helyszín hozzáadva",
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
            emptyText = "Még nincs gyógyító feladat hozzáadva",
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
            emptyText = "Még nincs bolti tárgy hozzáadva",
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

        Button(
            onClick = onCreateGame,
            enabled = !uiState.isLoading && validationError == null,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Mentés és tovább")
        }
        if (validationError != null) {
            Text(
                validationError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun DraftSection(
    title: String,
    emptyText: String,
    addText: String,
    isEmpty: Boolean,
    enabled: Boolean,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { isExpanded = !isExpanded },
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Szakasz bezárása" else "Szakasz megnyitása",
                    modifier = Modifier.rotate(arrowRotation),
                )
            }
        }
        if (isExpanded) {
            OutlinedButton(onClick = onAdd, enabled = enabled) {
                Text(addText)
            }
            if (isEmpty) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = location.name,
                    onValueChange = { onLocationNameChange(location.localId, it) },
                    label = { Text("Helyszín neve") },
                    singleLine = true,
                    isError = location.name.trim().isEmpty(),
                    supportingText = {
                        if (location.name.trim().isEmpty()) {
                            Text("Kötelező")
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onRemoveLocation(location.localId) }, enabled = enabled) {
                    Text("Törlés")
                }
            }
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
            OutlinedButton(onClick = { onAddTask(location.localId) }, enabled = enabled) {
                Text("Feladat hozzáadása")
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = task.text,
            onValueChange = { onTextChange(task.localId, it) },
            label = { Text("Feladat szövege") },
            isError = task.text.trim().isEmpty(),
            supportingText = {
                if (task.text.trim().isEmpty()) {
                    Text("Kötelező")
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = task.solution,
            onValueChange = { onSolutionChange(task.localId, it) },
            label = { Text("Megoldás") },
            singleLine = true,
            isError = task.solution.trim().isEmpty(),
            supportingText = {
                if (task.solution.trim().isEmpty()) {
                    Text("Kötelező")
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.isMiniBoss,
                    onCheckedChange = { onMiniBossChange(task.localId, it) },
                    enabled = enabled,
                )
                Text("Mini boss")
            }
            TextButton(onClick = { onRemove(task.localId) }, enabled = enabled) {
                Text("Feladat törlése")
            }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            OutlinedTextField(
                value = healingTask.text,
                onValueChange = { onTextChange(healingTask.localId, it) },
                label = { Text("Feladat szövege") },
                isError = healingTask.text.trim().isEmpty(),
                supportingText = {
                    if (healingTask.text.trim().isEmpty()) {
                        Text("Kötelező")
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = healingTask.solution,
                onValueChange = { onSolutionChange(healingTask.localId, it) },
                label = { Text("Megoldás") },
                singleLine = true,
                isError = healingTask.solution.trim().isEmpty(),
                supportingText = {
                    if (healingTask.solution.trim().isEmpty()) {
                        Text("Kötelező")
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { onRemove(healingTask.localId) },
                enabled = enabled,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Törlés")
            }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            OutlinedTextField(
                value = item.name,
                onValueChange = { onNameChange(item.localId, it) },
                label = { Text("Tárgy neve") },
                singleLine = true,
                isError = item.name.trim().isEmpty(),
                supportingText = {
                    if (item.name.trim().isEmpty()) {
                        Text("Kötelező")
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = item.price,
                    onValueChange = { onPriceChange(item.localId, it) },
                    label = { Text("Ár") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = item.price.toIntOrNull()?.let { it >= 0 } != true,
                    supportingText = {
                        if (item.price.toIntOrNull()?.let { it >= 0 } != true) {
                            Text("Nem negatív egész")
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.maxPerTeam,
                    onValueChange = { onMaxPerTeamChange(item.localId, it) },
                    label = { Text("Csapatonként") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = item.maxPerTeam.toIntOrNull()?.let { it > 0 } != true,
                    supportingText = {
                        if (item.maxPerTeam.toIntOrNull()?.let { it > 0 } != true) {
                            Text("Pozitív egész")
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }
            ItemEffectDropdown(
                selectedItemEffectId = item.itemEffectId,
                itemEffects = itemEffects,
                enabled = enabled,
                onSelect = { onEffectIdChange(item.localId, it) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { onRemove(item.localId) },
                enabled = enabled,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Törlés")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val label =
        selectedEffect?.let { "${it.id ?: "-"}: ${it.description.orEmpty()}" } ?: "Nincs effekt"

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { if (enabled) isExpanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Effekt") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled,
                ),
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Nincs effekt") },
                onClick = {
                    onSelect("")
                    isExpanded = false
                },
            )
            itemEffects.forEach { effect ->
                val id = effect.id?.toString().orEmpty()
                DropdownMenuItem(
                    text = { Text("${effect.id ?: "-"}: ${effect.description.orEmpty()}") },
                    enabled = id.isNotBlank(),
                    onClick = {
                        onSelect(id)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun GameRow(
    game: Game,
    isLoading: Boolean,
    onSelectGame: (Game) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(game.name ?: "Névtelen játék", style = MaterialTheme.typography.titleSmall)
                Text("ID ${game.id ?: "-"}", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onSelectGame(game) },
                enabled = !isLoading && game.id != null,
            ) {
                Text("Csatlakozás")
            }
        }
    }
}
