package com.thumperlog.app.ui

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.thumperlog.app.data.Session
import com.thumperlog.app.data.SessionRepository
import com.thumperlog.app.ui.theme.AmberColor
import com.thumperlog.app.ui.theme.BgColor
import com.thumperlog.app.ui.theme.MutedColor
import com.thumperlog.app.ui.theme.PanelColor
import com.thumperlog.app.ui.theme.RedColor
import com.thumperlog.app.ui.theme.TextColor

@Composable
fun DetailScreen(sessionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var session by remember { mutableStateOf<Session?>(null) }
    var label by remember { mutableStateOf("") }

    LaunchedEffect(sessionId) {
        session = SessionRepository.getSession(context, sessionId)
        label = session?.label ?: ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "← Back",
                color = AmberColor,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable {
                        session?.let {
                            if (label.isNotBlank() && label != it.label) {
                                SessionRepository.updateLabel(context, it.id, label)
                            }
                        }
                        onBack()
                    }
                    .padding(end = 10.dp)
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextColor,
                    unfocusedTextColor = TextColor,
                    focusedContainerColor = PanelColor,
                    unfocusedContainerColor = PanelColor
                )
            )
        }

        Spacer(Modifier.height(14.dp))

        val s = session
        if (s == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MutedColor)
            }
        } else {
            VibeGraph(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                samples = s.samples
            )

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Text("peak ${"%.2f".format(s.peak)} g", color = MutedColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                Text("avg ${"%.2f".format(s.avg)} g", color = MutedColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                Text("dur ${formatMs(s.duration)}", color = MutedColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Export CSV",
                    color = TextColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(PanelColor, RoundedCornerShape(8.dp))
                        .clickable {
                            val file = SessionRepository.exportCsv(context, s)
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share CSV"))
                        }
                        .padding(vertical = 12.dp)
                )
                Text(
                    "Delete",
                    color = RedColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(PanelColor, RoundedCornerShape(8.dp))
                        .clickable {
                            SessionRepository.deleteSession(context, s.id)
                            onBack()
                        }
                        .padding(vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
