package com.example.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class BadgeVariant {
    SUCCESS,
    WARNING,
    DANGER,
    ERROR,
    INFO,
    NEUTRAL
}

@Composable
fun RestoBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.NEUTRAL
) {
    val (bgColor, textColor) = when (variant) {
        BadgeVariant.SUCCESS -> RestoEmerald100 to RestoEmerald600
        BadgeVariant.WARNING -> RestoAmber100 to RestoAmber600
        BadgeVariant.DANGER, BadgeVariant.ERROR -> RestoRose100 to RestoRose600
        BadgeVariant.INFO -> RestoBlue100 to RestoBlue600
        BadgeVariant.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun RestoStatusBadge(
    text: String,
    variant: BadgeVariant = BadgeVariant.NEUTRAL,
    modifier: Modifier = Modifier
) {
    RestoBadge(text = text, modifier = modifier, variant = variant)
}
