package com.example.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RestoAmber500
import com.example.ui.theme.RestoBlue600
import com.example.ui.theme.RestoNavy900
import com.example.ui.theme.RestoRose600

enum class RestoButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINE,
    DANGER,
    GHOST
}

@Composable
fun RestoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RestoButtonVariant = RestoButtonVariant.PRIMARY,
    enabled: Boolean = true,
    testTag: String = "resto_button",
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val minModifier = modifier
        .defaultMinSize(minHeight = 48.dp)
        .testTag(testTag)

    when (variant) {
        RestoButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RestoAmber500,
                    contentColor = RestoNavy900,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                content = content
            )
        }
        RestoButtonVariant.SECONDARY -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                content = content
            )
        }
        RestoButtonVariant.OUTLINE -> {
            OutlinedButton(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                content = content
            )
        }
        RestoButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RestoRose600,
                    contentColor = Color.White
                ),
                content = content
            )
        }
        RestoButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}

@Composable
fun PosNumPadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .testTag("numpad_$label"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isAction) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isAction) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
