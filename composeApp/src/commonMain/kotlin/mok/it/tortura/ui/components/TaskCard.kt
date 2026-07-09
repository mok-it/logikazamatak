package mok.it.tortura.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mok.it.tortura.model.Task
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun TaskCard(
    task: Task,
    onChangeText: (String) -> Unit,
    onChangeSolution: (String) -> Unit,
    onDeleteTask: () -> Unit,
) {
    SectionCard(toned = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md),
            ) {
                AppTextField(
                    value = task.text,
                    onValueChange = onChangeText,
                    label = "Feladat szövege",
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = task.solution,
                    onValueChange = onChangeSolution,
                    label = "Feladat megoldása",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = onDeleteTask) {
                Icon(Icons.Filled.Delete, contentDescription = "Feladat törlése")
            }
        }
    }
}
