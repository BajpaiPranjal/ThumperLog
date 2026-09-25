package com.thumperlog.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.thumperlog.app.MainActivity
import com.thumperlog.app.R
import com.thumperlog.app.data.LiveState
import com.thumperlog.app.data.LiveVibration
import com.thumperlog.app.data.Sample
import com.thumperlog.app.data.Session
import com.thumperlog.app.data.SessionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

class VibrationService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "com.thumperlog.app.action.START"
        const val ACTION_STOP = "com.thumperlog.app.action.STOP"
        private const val CHANNEL_ID = "thumperlog_recording"
        private const val NOTIF_ID = 1001
        private const val LIVE_WINDOW_MS = 8000L
        private const val MAX_STORED_SAMPLES = 6000
    }

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private var usingLinear = false
    private var wakeLock: PowerManager.WakeLock? = null

    private var recording = false
    private var startTime = 0L
    private val samples = mutableListOf<Sample>()
    private val liveBuffer = mutableListOf<Sample>()
    private var peak = 0f
    private var sum = 0f
    private var count = 0

    // gravity low-pass filter state, used only when falling back to the raw accelerometer
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var gInit = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (linear != null) {
            sensor = linear
            usingLinear = true
        } else {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            usingLinear = false
        }
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (recording) return
        recording = true
        startTime = System.currentTimeMillis()
        samples.clear()
        liveBuffer.clear()
        peak = 0f; sum = 0f; count = 0
        gInit = false

        val notif = buildNotification("00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ThumperLog::RecordingLock")
        wakeLock?.acquire(6 * 60 * 60 * 1000L) // 6h safety cap

        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        LiveVibration.update(LiveState(isRecording = true))
    }

    private fun stopRecording() {
        if (!recording) {
            stopSelfSafely()
            return
        }
        recording = false
        sensorManager.unregisterListener(this)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        val duration = System.currentTimeMillis() - startTime
        if (samples.size >= 4) {
            val stored = decimate(samples, MAX_STORED_SAMPLES)
            val session = Session(
                id = System.currentTimeMillis().toString() + Random.nextInt(1000, 9999),
                label = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(startTime)),
                startTime = startTime,
                duration = duration,
                peak = peak,
                avg = if (count > 0) sum / count else 0f,
                samples = stored
            )
            SessionRepository.saveSession(applicationContext, session)
        }

        LiveVibration.update(LiveState(isRecording = false))
        stopSelfSafely()
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val mag = if (usingLinear) {
            magnitudeG(event.values[0], event.values[1], event.values[2])
        } else {
            magnitudeRawG(event.values[0], event.values[1], event.values[2])
        }
        val t = System.currentTimeMillis()

        liveBuffer.add(Sample(t, mag))
        val cutoff = t - LIVE_WINDOW_MS
        while (liveBuffer.isNotEmpty() && liveBuffer[0].tMs < cutoff) liveBuffer.removeAt(0)

        if (recording) {
            samples.add(Sample(t - startTime, mag))
            count++
            sum += mag
            if (mag > peak) peak = mag
            updateNotification(formatDuration(t - startTime))
        }

        LiveVibration.update(
            LiveState(
                isRecording = recording,
                currentMag = mag,
                elapsedMs = if (recording) t - startTime else 0L,
                peak = peak,
                avg = if (count > 0) sum / count else 0f,
                recentBuffer = liveBuffer.toList()
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun magnitudeG(x: Float, y: Float, z: Float): Float {
        return (sqrt((x * x + y * y + z * z).toDouble()) / 9.80665).toFloat()
    }

    private fun magnitudeRawG(x: Float, y: Float, z: Float): Float {
        val alpha = 0.8f
        if (!gInit) {
            gx = x; gy = y; gz = z; gInit = true
        }
        gx = alpha * gx + (1 - alpha) * x
        gy = alpha * gy + (1 - alpha) * y
        gz = alpha * gz + (1 - alpha) * z
        return magnitudeG(x - gx, y - gy, z - gz)
    }

    private fun decimate(list: List<Sample>, maxLen: Int): List<Sample> {
        if (list.size <= maxLen) return list
        val step = list.size.toDouble() / maxLen
        return (0 until maxLen).map { list[(it * step).toInt()] }
    }

    private fun formatDuration(ms: Long): String {
        val s = ms / 1000
        val m = s / 60
        val sec = s % 60
        return "%02d:%02d".format(m, sec)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Ride recording", NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shows while a vibration session is recording"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(elapsed: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ThumperLog — recording")
            .setContentText("Elapsed $elapsed")
            .setSmallIcon(R.drawable.ic_notif)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    private fun updateNotification(elapsed: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(elapsed))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recording) {
            sensorManager.unregisterListener(this)
            wakeLock?.let { if (it.isHeld) it.release() }
        }
    }
}
