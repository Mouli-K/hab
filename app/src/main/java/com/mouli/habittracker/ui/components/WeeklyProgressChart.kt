package com.mouli.habittracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mouli.habittracker.model.HabitCardUiModel
import com.mouli.habittracker.ui.theme.AccentBlue
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyProgressChart(
    cards: List<HabitCardUiModel>,
    modifier: Modifier = Modifier
) {
    val dailyProgress = remember(cards) {
        if (cards.isEmpty()) return@remember emptyList<Pair<String, Float>>()

        val days = cards.first().backlogDays.map { it.date }
        days.map { date ->
            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val progress = cards.map { card ->
                val day = card.backlogDays.find { it.date == date }
                if (day?.isFrozen == true) 1f
                else {
                    val amount = day?.amount ?: 0
                    (amount.toFloat() / card.targetValue.toFloat()).coerceIn(0f, 1f)
                }
            }.average().toFloat()
            dayLabel to progress
        }
    }

    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Weekly rhythm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "A quiet look at your progress over the last few days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyProgress.forEach { (label, progress) ->
                ChartBar(label = label, progress = progress)
            }
        }
    }
}

@Composable
private fun ChartBar(
    label: String,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "chart-bar-progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        if (progress >= 1f) AccentBlue else AccentBlue.copy(alpha = 0.6f)
                    )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
    }
}
