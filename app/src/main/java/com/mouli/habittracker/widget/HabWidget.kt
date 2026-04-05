package com.mouli.habittracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mouli.habittracker.MainActivity
import com.mouli.habittracker.R
import com.mouli.habittracker.data.local.HabDatabase
import com.mouli.habittracker.domain.FlexibleHabitEngine
import com.mouli.habittracker.model.MascotMood
import java.time.LocalDate

class HabWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = HabDatabase.getInstance(context)
        val profile = database.profileDao().getProfile()
        val habits = database.habitDao().getHabits()
        val logs = database.habitLogDao().getLogs()
        val today = LocalDate.now()
        val cards = habits.map { habit ->
            FlexibleHabitEngine.buildHabitCard(
                habit = habit,
                logs = logs.filter { it.habitId == habit.id },
                today = today
            )
        }

        provideContent {
            WidgetContent(
                name = profile?.displayName?.takeIf { it.isNotBlank() } ?: "Friend",
                completedCount = cards.count { it.isComplete },
                totalCount = cards.size,
                mood = FlexibleHabitEngine.pickMood(cards),
                focusLabel = cards.firstOrNull { !it.isComplete }?.title ?: cards.firstOrNull()?.title.orEmpty()
            )
        }
    }
}

@Composable
private fun WidgetContent(
        name: String,
        completedCount: Int,
        totalCount: Int,
        mood: MascotMood,
        focusLabel: String
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.White))
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.mascot_panda),
                contentDescription = "Panda mascot",
                modifier = GlanceModifier.fillMaxSize().padding(4.dp)
            )
        }
    }

private fun widgetMoodLabel(mood: MascotMood): String = when (mood) {
    MascotMood.SPARKLY -> "Sparkly panda"
    MascotMood.CHEERING -> "Cheering panda"
    MascotMood.CALM -> "Calm panda"
    MascotMood.RESTING -> "Resting panda"
}

class HabWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabWidget()
}
