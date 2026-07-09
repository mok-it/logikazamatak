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
import mok.it.tortura.model.Student
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun StudentCard(
    student: Student,
    onChangeName: (String) -> Unit,
    onChangeGroup: (String) -> Unit,
    onDeleteStudent: () -> Unit,
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
                    value = student.name,
                    onValueChange = onChangeName,
                    label = "Diák neve",
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = student.group,
                    onValueChange = onChangeGroup,
                    label = "Csoport",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = onDeleteStudent) {
                Icon(Icons.Filled.Delete, contentDescription = "Diák törlése")
            }
        }
    }
}
