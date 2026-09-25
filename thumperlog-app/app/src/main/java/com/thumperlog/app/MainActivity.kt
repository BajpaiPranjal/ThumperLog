package com.thumperlog.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.thumperlog.app.service.VibrationService
import com.thumperlog.app.ui.DetailScreen
import com.thumperlog.app.ui.RecordScreen
import com.thumperlog.app.ui.SessionsScreen
import com.thumperlog.app.ui.theme.ThumperLogTheme

sealed class Screen {
    object Record : Screen()
    object Sessions : Screen()
    data class Detail(val sessionId: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            ThumperLogTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Record) }

                when (val s = screen) {
                    is Screen.Record -> RecordScreen(
                        onStart = {
                            startService(
                                Intent(this, VibrationService::class.java)
                                    .setAction(VibrationService.ACTION_START)
                            )
                        },
                        onStop = {
                            startService(
                                Intent(this, VibrationService::class.java)
                                    .setAction(VibrationService.ACTION_STOP)
                            )
                        },
                        onOpenSessions = { screen = Screen.Sessions }
                    )
                    is Screen.Sessions -> SessionsScreen(
                        onOpenSession = { id -> screen = Screen.Detail(id) },
                        onOpenRecord = { screen = Screen.Record }
                    )
                    is Screen.Detail -> DetailScreen(
                        sessionId = s.sessionId,
                        onBack = { screen = Screen.Sessions }
                    )
                }
            }
        }
    }
}
