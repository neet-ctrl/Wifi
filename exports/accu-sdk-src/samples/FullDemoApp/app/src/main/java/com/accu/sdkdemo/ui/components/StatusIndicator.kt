package com.accu.sdkdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accu.sdkdemo.ui.theme.AccuAmber
import com.accu.sdkdemo.ui.theme.AccuGreen
import com.accu.sdkdemo.ui.theme.AccuRed

enum class StatusColor { GREEN, YELLOW, RED, GREY }

@Composable
fun StatusDot(color: StatusColor, modifier: Modifier = Modifier) {
    val c = when (color) {
        StatusColor.GREEN  -> AccuGreen
        StatusColor.YELLOW -> AccuAmber
        StatusColor.RED    -> AccuRed
        StatusColor.GREY   -> Color.Gray
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(c)
    )
}

@Composable
fun StatusRow(
    label: String,
    value: String,
    statusColor: StatusColor,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(statusColor)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = when (statusColor) {
                StatusColor.GREEN  -> AccuGreen
                StatusColor.YELLOW -> AccuAmber
                StatusColor.RED    -> AccuRed
                StatusColor.GREY   -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun StatusBadge(text: String, color: StatusColor, modifier: Modifier = Modifier) {
    val bgColor = when (color) {
        StatusColor.GREEN  -> AccuGreen.copy(alpha = 0.15f)
        StatusColor.YELLOW -> AccuAmber.copy(alpha = 0.15f)
        StatusColor.RED    -> AccuRed.copy(alpha = 0.15f)
        StatusColor.GREY   -> Color.Gray.copy(alpha = 0.15f)
    }
    val textColor = when (color) {
        StatusColor.GREEN  -> AccuGreen
        StatusColor.YELLOW -> AccuAmber
        StatusColor.RED    -> AccuRed
        StatusColor.GREY   -> Color.Gray
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.padding(vertical = 8.dp),
    )
}
