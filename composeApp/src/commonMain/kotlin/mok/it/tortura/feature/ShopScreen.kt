package mok.it.tortura.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Item
import mok.it.tortura.model.Team
import mok.it.tortura.ui.components.*
import mok.it.tortura.ui.theme.AppTheme
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun ShopScreen(
    activeGameName: String,
    activeLocationName: String?,
    uiState: ShopUiState = ShopUiState(),
    onLoad: () -> Unit = {},
    onSelectTeam: (Long) -> Unit = {},
    onScoreAdjustmentInputChange: (String) -> Unit = {},
    onApplyScoreAdjustment: () -> Unit = {},
    onTargetChange: (Long, Long) -> Unit = { _, _ -> },
    onPurchase: (Long) -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
    onChangeLocation: () -> Unit = {},
) {
    val spacing = AppThemeTokens.spacing
    val selectedTeam = remember(uiState.selectedTeamId, uiState.teams) {
        uiState.teams.firstOrNull { it.id == uiState.selectedTeamId }
    }
    var purchaseDialogItemId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        onLoad()
    }

    val purchaseDialogItem = remember(purchaseDialogItemId, uiState.itemRows) {
        uiState.itemRows.firstOrNull { it.item.id == purchaseDialogItemId }
    }

    PageScaffold(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        PageHeader(
            title = "Bolt",
            description = buildShopPageDescription(
                activeGameName = activeGameName,
                activeLocationName = activeLocationName,
            ),
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                    leadingIcon = { NavigateBackIcon() },
                )
                AppButton(
                    text = currentLocationButtonLabel(activeLocationName),
                    onClick = onChangeLocation,
                    style = AppButtonStyle.Secondary,
                    leadingIcon = { ChangeLocationIcon() },
                )
            },
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        TransientToastEffect(
            message = uiState.message,
            errorMessage = uiState.errorMessage,
            onConsumed = onClearMessages,
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val splitLayout = maxWidth >= 960.dp

            if (splitLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        TeamBalanceSection(
                            teams = uiState.teams,
                            selectedTeam = selectedTeam,
                            teamScore = uiState.selectedTeamScore,
                            teamSpent = uiState.selectedTeamSpent,
                            teamBudget = uiState.selectedTeamBudget,
                            scoreAdjustmentInput = uiState.scoreAdjustmentInput,
                            isLoading = uiState.isLoading,
                            onSelectTeam = onSelectTeam,
                            onScoreAdjustmentInputChange = onScoreAdjustmentInputChange,
                            onApplyScoreAdjustment = onApplyScoreAdjustment,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.35f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        ShopCatalogSection(
                            itemRows = uiState.itemRows,
                            selectedTeam = selectedTeam,
                            availableBudget = uiState.selectedTeamBudget,
                            isLoading = uiState.isLoading,
                            onPurchaseClick = { itemId -> purchaseDialogItemId = itemId },
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
                    TeamBalanceSection(
                        teams = uiState.teams,
                        selectedTeam = selectedTeam,
                        teamScore = uiState.selectedTeamScore,
                        teamSpent = uiState.selectedTeamSpent,
                        teamBudget = uiState.selectedTeamBudget,
                        scoreAdjustmentInput = uiState.scoreAdjustmentInput,
                        isLoading = uiState.isLoading,
                        onSelectTeam = onSelectTeam,
                        onScoreAdjustmentInputChange = onScoreAdjustmentInputChange,
                        onApplyScoreAdjustment = onApplyScoreAdjustment,
                    )
                    ShopCatalogSection(
                        itemRows = uiState.itemRows,
                        selectedTeam = selectedTeam,
                        availableBudget = uiState.selectedTeamBudget,
                        isLoading = uiState.isLoading,
                        onPurchaseClick = { itemId -> purchaseDialogItemId = itemId },
                    )
                }
            }
        }

        purchaseDialogItem?.let { itemRow ->
            PurchaseDialog(
                itemRow = itemRow,
                selectedTeam = selectedTeam,
                availableBudget = uiState.selectedTeamBudget,
                isLoading = uiState.isLoading,
                onDismiss = { purchaseDialogItemId = null },
                onTargetChange = onTargetChange,
                onPurchase = { itemId ->
                    onPurchase(itemId)
                    purchaseDialogItemId = null
                },
            )
        }
    }
}

@Composable
private fun TeamBalanceSection(
    teams: List<Team>,
    selectedTeam: Team?,
    teamScore: Int?,
    teamSpent: Int?,
    teamBudget: Int?,
    scoreAdjustmentInput: String,
    isLoading: Boolean,
    onSelectTeam: (Long) -> Unit,
    onScoreAdjustmentInputChange: (String) -> Unit,
    onApplyScoreAdjustment: () -> Unit,
) {
    val spacing = AppThemeTokens.spacing
    val teamOptions = remember(teams) {
        teams.mapNotNull { team ->
            val teamId = team.id ?: return@mapNotNull null
            AppSelectOption(
                value = teamId.toString(),
                label = team.name ?: "Csapat #$teamId",
            )
        }
    }

    SectionCard(toned = true) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 720.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TeamComboBox(
                        selectedLabel = selectedTeam?.name ?: "Válassz csapatot",
                        options = teamOptions,
                        enabled = !isLoading && teamOptions.isNotEmpty(),
                        onSelectTeam = onSelectTeam,
                        modifier = Modifier.weight(1f),
                    )
                    TeamInfoLine(
                        selectedTeam = selectedTeam,
                        teamScore = teamScore,
                        teamSpent = teamSpent,
                        teamBudget = teamBudget,
                    )
                }
                ScoreAdjustmentRow(
                    value = scoreAdjustmentInput,
                    enabled = !isLoading && selectedTeam != null,
                    onValueChange = onScoreAdjustmentInputChange,
                    onApply = onApplyScoreAdjustment,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Bolt keret",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Egyenleg: ${teamBudget ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppThemeTokens.colors.textPrimary,
                        )
                    }
                    TeamComboBox(
                        selectedLabel = selectedTeam?.name ?: "Válassz csapatot",
                        options = teamOptions,
                        enabled = !isLoading && teamOptions.isNotEmpty(),
                        onSelectTeam = onSelectTeam,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TeamInfoLine(
                        selectedTeam = selectedTeam,
                        teamScore = teamScore,
                        teamSpent = teamSpent,
                        teamBudget = teamBudget,
                    )
                    ScoreAdjustmentRow(
                        value = scoreAdjustmentInput,
                        enabled = !isLoading && selectedTeam != null,
                        onValueChange = onScoreAdjustmentInputChange,
                        onApply = onApplyScoreAdjustment,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamInfoLine(
    selectedTeam: Team?,
    teamScore: Int?,
    teamSpent: Int?,
    teamBudget: Int?,
) {
    val colors = AppThemeTokens.colors
    val score = teamScore ?: 0
    val spent = teamSpent ?: 0
    val budget = teamBudget ?: 0
    val teamName = selectedTeam?.name ?: "Nincs kiválasztott csapat"
    val teamId = selectedTeam?.id?.toString() ?: "-"

    Text(
        text = "$teamName • ID $teamId • Pont $score • Elköltött $spent • Egyenleg $budget",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
    )
}

@Composable
private fun TeamComboBox(
    selectedLabel: String,
    options: List<AppSelectOption>,
    enabled: Boolean,
    onSelectTeam: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        AppButton(
            onClick = { if (enabled) expanded = true },
            style = AppButtonStyle.Secondary,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selectedLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Csapat választása",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    enabled = option.enabled,
                    onClick = {
                        expanded = false
                        option.value.toLongOrNull()?.let(onSelectTeam)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScoreAdjustmentRow(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = "Pontkorrekció (-/+)",
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = "Alkalmaz",
            onClick = onApply,
            enabled = enabled && value.isNotBlank() && value != "-",
            style = AppButtonStyle.Secondary,
            leadingIcon = { SaveIcon() },
        )
    }
}

@Composable
private fun ShopCatalogSection(
    itemRows: List<ShopItemRow>,
    selectedTeam: Team?,
    availableBudget: Int?,
    isLoading: Boolean,
    onPurchaseClick: (Long) -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(
            title = "Shop tárgyak",
        )

        if (itemRows.isEmpty() && !isLoading) {
            EmptyState(
                title = "Nincs shop tárgy",
                description = "Ehhez a játékhoz még nincs felvett shop tárgy.",
            )
        } else {
            ShopItemsTable(
                itemRows = itemRows,
                selectedTeam = selectedTeam,
                availableBudget = availableBudget,
                isLoading = isLoading,
                onPurchaseClick = onPurchaseClick,
            )
        }
    }
}

@Composable
private fun ShopItemsTable(
    itemRows: List<ShopItemRow>,
    selectedTeam: Team?,
    availableBudget: Int?,
    isLoading: Boolean,
    onPurchaseClick: (Long) -> Unit,
) {
    SectionCard {
        ShopTableHeader()
        itemRows.forEachIndexed { index, itemRow ->
            if (index > 0) {
                HorizontalDivider()
            }
            ShopTableRow(
                itemRow = itemRow,
                selectedTeam = selectedTeam,
                availableBudget = availableBudget,
                isLoading = isLoading,
                onPurchaseClick = onPurchaseClick,
            )
        }
    }
}

@Composable
private fun ShopTableHeader() {
    val colors = AppThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableCell(
            text = "Név",
            modifier = Modifier.weight(2.4f),
            color = colors.textSecondary,
        )
        TableCell(
            text = "Ár",
            modifier = Modifier.weight(0.8f),
            color = colors.textSecondary,
        )
        TableCell(
            text = "Megvett / összes",
            modifier = Modifier.weight(1.4f),
            color = colors.textSecondary,
        )
        TableCell(
            text = "Művelet",
            modifier = Modifier.weight(1.1f),
            color = colors.textSecondary,
            alignEnd = true,
        )
    }
}

@Composable
private fun ShopTableRow(
    itemRow: ShopItemRow,
    selectedTeam: Team?,
    availableBudget: Int?,
    isLoading: Boolean,
    onPurchaseClick: (Long) -> Unit,
) {
    val itemId = itemRow.item.id
    val price = itemRow.item.price ?: 0
    val hasRemainingStock = itemRow.remainingStock?.let { it > 0 } ?: true
    val hasEnoughMoney = availableBudget?.let { it >= price } ?: false
    val purchaseEnabled = !isLoading &&
        itemId != null &&
        selectedTeam?.id != null &&
        itemRow.isEligibleForPurchase &&
        hasRemainingStock &&
        hasEnoughMoney
    val totalLimit = itemRow.totalAvailableCount?.toString() ?: "∞"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(2.4f),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.xs),
        ) {
            Text(
                text = itemRow.item.name ?: "Tárgy #${itemId ?: "-"}",
                style = MaterialTheme.typography.titleMedium,
            )
            itemRow.effectDescription?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppThemeTokens.colors.textSecondary,
                )
            }
            itemRow.purchaseBlockedReason?.takeIf { it.isNotBlank() }?.let { blockedReason ->
                Text(
                    text = blockedReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppThemeTokens.colors.warning,
                )
            }
        }
        TableCell(
            text = price.toString(),
            modifier = Modifier.weight(0.8f),
        )
        TableCell(
            text = "${itemRow.purchasedCount}/$totalLimit",
            modifier = Modifier.weight(1.4f),
        )
        Row(
            modifier = Modifier.weight(1.1f),
            horizontalArrangement = Arrangement.End,
        ) {
            AppButton(
                text = "Vásárlás",
                onClick = { itemId?.let(onPurchaseClick) },
                enabled = purchaseEnabled,
                leadingIcon = { ShopIcon() },
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color = AppThemeTokens.colors.textPrimary,
    alignEnd: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

@Composable
private fun PurchaseDialog(
    itemRow: ShopItemRow,
    selectedTeam: Team?,
    availableBudget: Int?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onTargetChange: (Long, Long) -> Unit,
    onPurchase: (Long) -> Unit,
) {
    val itemId = itemRow.item.id ?: return
    val price = itemRow.item.price ?: 0
    val requiresTarget = itemRow.targetType != ShopTargetType.NONE
    val hasEnoughMoney = availableBudget?.let { it >= price } ?: false
    val hasRemainingStock = itemRow.remainingStock?.let { it > 0 } ?: true
    val canConfirmPurchase = !isLoading &&
        selectedTeam?.id != null &&
        itemRow.isEligibleForPurchase &&
        hasEnoughMoney &&
        hasRemainingStock &&
        (!requiresTarget || itemRow.selectedTargetId != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(itemRow.item.name ?: "Tárgy vásárlása")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.lg)) {
                Text("Ár: $price")
                Text("Elérhető egyenleg: ${availableBudget ?: 0}")

                if (requiresTarget) {
                    TargetField(
                        itemId = itemId,
                        itemRow = itemRow,
                        enabled = !isLoading,
                        onTargetChange = onTargetChange,
                    )
                }

                itemRow.purchaseBlockedReason?.let { blockedReason ->
                    Text(
                        text = blockedReason,
                        color = AppThemeTokens.colors.warning,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (!hasEnoughMoney) {
                    Text(
                        text = "A csapat költhető pontkerete nem elég ehhez a vásárláshoz.",
                        color = AppThemeTokens.colors.danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (!hasRemainingStock) {
                    Text(
                        text = "Ez a csapat elérte a vásárlási limitet ennél a tárgynál.",
                        color = AppThemeTokens.colors.warning,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Vásárlás",
                onClick = { onPurchase(itemId) },
                enabled = canConfirmPurchase,
                leadingIcon = { ShopIcon() },
            )
        },
        dismissButton = {
            AppButton(
                text = "Mégse",
                onClick = onDismiss,
                style = AppButtonStyle.Ghost,
            )
        },
    )
}

@Composable
private fun TargetField(
    itemId: Long,
    itemRow: ShopItemRow,
    enabled: Boolean,
    onTargetChange: (Long, Long) -> Unit,
) {
    var expanded by rememberSaveable(itemId) { mutableStateOf(false) }
    val selectedTarget = itemRow.targetOptions.firstOrNull { it.id == itemRow.selectedTargetId }
    val options = remember(itemRow.targetOptions) {
        itemRow.targetOptions.map { target ->
            AppSelectOption(
                value = target.id.toString(),
                label = target.label,
            )
        }
    }

    AppSelectField(
        value = selectedTarget?.label ?: "",
        label = targetLabel(itemRow.targetType),
        expanded = expanded,
        options = options,
        onExpandedChange = { expanded = it },
        onSelect = { option ->
            option.value.toLongOrNull()?.let { targetId ->
                onTargetChange(itemId, targetId)
            }
        },
        enabled = enabled && options.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun targetLabel(targetType: ShopTargetType): String = when (targetType) {
    ShopTargetType.NONE -> "Célpont"
    ShopTargetType.TASK -> "Feladat kiválasztása"
    ShopTargetType.LOCATION -> "Helyszín kiválasztása"
    ShopTargetType.MINIBOSS_TASK -> "Miniboss kiválasztása"
}

private fun buildShopPageDescription(
    activeGameName: String,
    activeLocationName: String?,
): String = activeLocationName?.let { locationName ->
    "$activeGameName • aktív helyszín: $locationName"
} ?: "$activeGameName • még nincs kiválasztott helyszín"

private fun currentLocationButtonLabel(activeLocationName: String?): String =
    "Helyszín: ${activeLocationName ?: "nincs kiválasztva"}"

@Preview
@Composable
private fun ShopScreenPreview() {
    AppTheme {
        ShopScreen(
            activeGameName = "Logikazamata 2026",
            activeLocationName = "Nagyterem",
            uiState = ShopUiState(
                teams = listOf(
                    Team(id = 12, name = "Kék csapat"),
                    Team(id = 13, name = "Zöld csapat"),
                ),
                selectedTeamId = 12,
                selectedTeamScore = 18,
                selectedTeamSpent = 8,
                selectedTeamBudget = 10,
                itemRows = listOf(
                    ShopItemRow(
                        item = Item(
                            id = 1,
                            name = "Feladat duplázó",
                            price = 5,
                            maxPerTeam = 2,
                        ),
                        effectDescription = "feladatduplázó",
                        targetType = ShopTargetType.TASK,
                        targetOptions = listOf(
                            ShopTargetOption(31, "Mi a következő szám?"),
                            ShopTargetOption(32, "Találd meg a mintát"),
                        ),
                        purchasedCount = 1,
                        remainingStock = 1,
                    ),
                    ShopItemRow(
                        item = Item(
                            id = 2,
                            name = "Terület duplázó visszamenőleg",
                            price = 10,
                            maxPerTeam = 1,
                        ),
                        effectDescription = "területduplázó visszamenőleg",
                        targetType = ShopTargetType.LOCATION,
                        targetOptions = listOf(
                            ShopTargetOption(61, "Könyvtár"),
                            ShopTargetOption(62, "Udvar"),
                        ),
                        selectedTargetId = 61,
                        purchasedCount = 0,
                        remainingStock = 1,
                    ),
                ),
            ),
        )
    }
}
