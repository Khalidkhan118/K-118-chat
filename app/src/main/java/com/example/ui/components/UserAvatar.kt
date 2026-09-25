package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.SlateOffline

@Composable
fun UserAvatar(
    photoUrl: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean? = null,
    showOnlineBadge: Boolean = true,
    hasStatus: Boolean = false,
    hasUnviewedStatus: Boolean = false,
    statusCount: Int = 1
) {
    val totalSize = if (hasStatus) size + 8.dp else size

    Box(
        modifier = modifier
            .size(totalSize)
            .testTag("user_avatar_${displayName.lowercase().replace(" ", "_")}"),
        contentAlignment = Alignment.Center
    ) {
        // WhatsApp-style status ring
        if (hasStatus) {
            val ringColor = if (hasUnviewedStatus) EmeraldOnline else Color(0xFF8696A0).copy(alpha = 0.6f)
            Canvas(modifier = Modifier.size(totalSize)) {
                val strokeWidth = 2.5.dp.toPx()
                val radius = (this.size.minDimension - strokeWidth) / 2f
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(radius * 2f, radius * 2f)

                if (statusCount <= 1) {
                    drawCircle(
                        color = ringColor,
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    val count = statusCount.coerceIn(2, 20)
                    val gapDegree = (24f / count).coerceIn(4f, 10f)
                    val sweepDegree = (360f / count) - gapDegree

                    for (i in 0 until count) {
                        val startAngle = -90f + i * (sweepDegree + gapDegree) + (gapDegree / 2f)
                        drawArc(
                            color = ringColor,
                            startAngle = startAngle,
                            sweepAngle = sweepDegree,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        val initials = displayName.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "U" }

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        ) {
            if (photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar for $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = (size.value * 0.38).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Online / Offline Status indicator badge
        if (showOnlineBadge && isOnline != null) {
            val badgeSize = (size * 0.28f).coerceAtLeast(10.dp)
            val badgeColor = if (isOnline) EmeraldOnline else SlateOffline
            val borderColor = MaterialTheme.colorScheme.surface

            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = if (hasStatus) 2.dp else 1.dp, y = if (hasStatus) 2.dp else 1.dp)
                    .border(2.dp, borderColor, CircleShape)
                    .background(badgeColor, CircleShape)
                    .testTag("status_indicator_${if (isOnline) "online" else "offline"}")
            )
        }
    }
}
