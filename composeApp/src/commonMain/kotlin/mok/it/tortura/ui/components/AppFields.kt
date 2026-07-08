package mok.it.tortura.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import mok.it.tortura.ui.theme.AppThemeTokens

data class AppSelectOption(
    val value: String,
    val label: String,
    val enabled: Boolean = true,
)

fun sanitizeNumericInput(value: String): String = value.filter(Char::isDigit)

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AppThemeTokens.colors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        readOnly = readOnly,
        isError = isError,
        keyboardOptions = keyboardOptions,
        label = { Text(label) },
        supportingText = supportingText?.let { text ->
            {
                Text(
                    text = text,
                    color = if (isError) colors.danger else colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.borderSubtle,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.accent,
            errorBorderColor = colors.danger,
            errorLabelColor = colors.danger,
            errorCursorColor = colors.danger,
            focusedContainerColor = colors.surfaceRaised,
            unfocusedContainerColor = colors.surfaceRaised,
            disabledContainerColor = colors.surfaceMuted,
        ),
    )
}

@Composable
fun AppNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    AppTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeNumericInput(it)) },
        label = label,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectField(
    value: String,
    label: String,
    expanded: Boolean,
    options: List<AppSelectOption>,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AppSelectOption) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(it) },
        modifier = modifier,
    ) {
        AppTextField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled,
                ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    enabled = option.enabled,
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}
