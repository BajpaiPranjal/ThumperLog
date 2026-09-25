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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thumperlog.app.data.Session
import com.thumperlog.app.data.SessionRepository
import com.thumperlog.app.ui.theme.AmberColor
import com.thumperlog.app.ui.theme.BgColor
import com.thumperlog.app.ui.theme.MutedColor
import com.thumperlog.app.ui.theme.PanelColor
import com.thumperlog.app.ui.theme.TextColor

@Composable
fun SessionsScreen(
    onOpenSession: (String) -> Unit,
    onOpenRecord: () -> Unit
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }

    LaunchedEffect(Unit) {
        sessions = SessionRepository.listSessions(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sessions", color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "Record",
                color = AmberColor,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onOpenRecord() }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions yet. Record a ride to see it here.", color = MutedColor, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions) { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(PanelColor, RoundedCornerShape(10.dp))
                            .clickable { onOpenSession(s.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(s.label, color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${formatMs(s.duration)} · ${s.samples.size} pts",
                                color = MutedColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            "${"%.2f".format(s.peak)} g peak",
                            color = AmberColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
