package com.example.expenserecord.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState

@Composable
fun Modifier.focusHighlight(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier {
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1f, label = "focusScale")
    // val elevation by animateDpAsState(targetValue = if (isFocused) 8.dp else 0.dp, label = "focusElevation")
    return this
        .scale(scale)
        // .shadow(elevation = elevation, shape = shape, clip = false)
}
