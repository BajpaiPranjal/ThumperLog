package com.thumperlog.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.thumperlog.app.data.Sample
import com.thumperlog.app.ui.theme.AmberColor
import com.thumperlog.app.ui.theme.LineColor
import com.thumperlog.app.ui.theme.PanelColor
import com.thumperlog.app.ui.theme.RedColor

@Composable
fun VibeGraph(
    modifier: Modifier = Modifier,
    samples: List<Sample>,
    liveColorWhenActive: Boolean = false
) {
    Canvas(
        modifier = modifier
            .background(PanelColor, RoundedCornerShape(10.dp))
            .border(1.dp, LineColor, RoundedCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height

        val rows = 4
        for (i in 1 until rows) {
            val y = h / rows * i
            drawLine(LineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        if (samples.size < 2) return@Canvas

        val minT = samples.first().tMs
        val maxT = samples.last().tMs.coerceAtLeast(minT + 1)
        var maxMag = 1f
        samples.forEach { if (it.mag > maxMag) maxMag = it.mag }
        maxMag *= 1.15f

        val path = Path()
        samples.forEachIndexed { i, s ->
            val x = ((s.tMs - minT).toFloat() / (maxT - minT).toFloat()) * w
            val y = h - (s.mag / maxMag) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = if (liveColorWhenActive) RedColor else AmberColor,
            style = Stroke(width = 4f)
        )
    }
}
