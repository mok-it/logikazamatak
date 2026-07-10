package mok.it.tortura.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamProgressSummary
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun TeamInfoDisclosure(
    team: Team?,
    progress: TeamProgressSummary?,
    modifier: Modifier = Modifier,
) {
    if (team == null) return

    var isOpen by rememberSaveable(team.id) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm),
    ) {
        AppButton(
            text = if (isOpen) "Részletek elrejtése" else "Csapatinfó",
            onClick = { isOpen = !isOpen },
            style = AppButtonStyle.Secondary,
            leadingIcon = {
                Icon(
                    imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            },
        )

        if (isOpen) {
            SectionCard(toned = true) {
                progress?.let {
                    Column(verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm)) {
                        TeamInfoMetricRow("Pont", it.points.toString())
                        TeamInfoMetricRow("Pénz", it.money.toString())
                        TeamInfoMetricRow("Elköltött pont", it.spent.toString())
                        TeamInfoMetricRow("Megoldott feladatok", "${it.solvedTasks} / ${it.totalTasks}")
                        TeamInfoMetricRow("Legyőzött minibossok", "${it.defeatedMiniBosses} / ${it.totalMiniBosses}")
                    }
                }

                Text(
                    text = "Csapattagok",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppThemeTokens.colors.textPrimary,
                )
                if (team.students.isEmpty()) {
                    Text(
                        text = "Nincs hozzárendelt csapattag.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppThemeTokens.colors.textSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.xs)) {
                        team.students.forEach { student ->
                            Text(
                                text = buildString {
                                    append(student.name.ifBlank { "Névtelen diák" })
                                    if (student.group.isNotBlank()) {
                                        append(" • ")
                                        append(student.group)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppThemeTokens.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamInfoMetricRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppThemeTokens.colors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = AppThemeTokens.colors.textPrimary,
        )
    }
}
