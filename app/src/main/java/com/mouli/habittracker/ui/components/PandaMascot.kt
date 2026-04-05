package com.mouli.habittracker.ui.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mouli.habittracker.R
import com.mouli.habittracker.model.MascotMood
import com.mouli.habittracker.ui.theme.AccentBlue
import com.mouli.habittracker.ui.theme.CrystalBlue
import com.mouli.habittracker.ui.theme.IceBlue

@Composable
fun PandaMascot(
    modifier: Modifier = Modifier,
    mood: MascotMood,
    mascotSize: Dp = 128.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "panda-breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panda-scale"
    )

    val frameColor = when (mood) {
        MascotMood.SPARKLY -> AccentBlue
        MascotMood.CHEERING -> CrystalBlue
        MascotMood.CALM -> IceBlue
        MascotMood.RESTING -> IceBlue.copy(alpha = 0.72f)
    }

    Box(
        modifier = modifier
            .size(mascotSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(frameColor, shape = CircleShape)
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.mascot_panda),
            contentDescription = "Hab panda mascot",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}
