package com.warehouse.rfid.edge.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.WarehouseTheme

private val ButtonMinHeight = 48.dp
private val ButtonContentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ButtonLoadingSpinner(color: Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = color,
    )
}

/** Filled, brand-colored. At most one per screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable RowScope.() -> Unit)? = null,
) {
    val glowColor = if (enabled && !isLoading) {
        WarehouseTheme.extendedColors.glow.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .shadow(
                elevation = 10.dp,
                shape = MaterialTheme.shapes.medium,
                ambientColor = glowColor,
                spotColor = glowColor,
            ),
        enabled = enabled && !isLoading,
        contentPadding = ButtonContentPadding,
    ) {
        if (isLoading) {
            ButtonLoadingSpinner(MaterialTheme.colorScheme.onPrimary)
        } else {
            leadingIcon?.invoke(this)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Outlined, neutral. For the second action on a screen. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled && !isLoading,
        contentPadding = ButtonContentPadding,
    ) {
        if (isLoading) {
            ButtonLoadingSpinner(MaterialTheme.colorScheme.primary)
        } else {
            leadingIcon?.invoke(this)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** No container. For low-emphasis actions like "Cancel". */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        contentPadding = ButtonContentPadding,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Filled, error-colored. For irreversible/destructive actions only. */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled && !isLoading,
        contentPadding = ButtonContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        if (isLoading) {
            ButtonLoadingSpinner(MaterialTheme.colorScheme.onError)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
