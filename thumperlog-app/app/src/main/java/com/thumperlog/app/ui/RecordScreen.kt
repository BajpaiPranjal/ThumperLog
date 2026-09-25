package com.thumperlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thumperlog.app.data.LiveVibration
import com.thumperlog.app.ui.theme.AmberColor
import com.thumperlog.app.ui.theme.BgColor
import com.thumperlog.app.ui.theme.MutedColor
import com.thumperlog.app.ui.theme.PanelColor
import com.thumperlog.app.ui.theme.PanelColor2
import com.thumperlog.app.ui.theme.RedColor
import com.thumperlog.app.ui.theme.TextColor

@Composable
fun RecordScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSessions: () -> Unit
) {
    val state by LiveVibration.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ThumperLog", color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                if (state.isRecording) "recording" else "idle",
                color = if (state.isRecording) RedColor else MutedColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.2f".format(state.currentMag),
                    color = TextColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 56.sp
                )
                Text(" g", color = MutedColor, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            }
            Text(
                formatMs(state.elapsedMs),
                color = MutedColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        VibeGraph(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp),
            samples = state.recentBuffer,
            liveColorWhenActive = state.isRecording
        )

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "peak ${"%.2f".format(state.peak)} g",
                color = MutedColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Text(
                "avg ${"%.2f".format(state.avg)} g",
                color = MutedColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PanelColor2)
                    .clickable { if (state.isRecording) onStop() else onStart() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(if (state.isRecording) RoundedCornerShape(6.dp) else CircleShape)
                        .background(if (state.isRecording) RedColor else AmberColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.isRecording) "Recording — tap to stop" else "Mount the phone, then tap to start recording",
                color = MutedColor,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(PanelColor, RoundedCornerShape(10.dp))
                .clickable { onOpenSessions() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("View sessions", color = AmberColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

fun formatMs(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}
