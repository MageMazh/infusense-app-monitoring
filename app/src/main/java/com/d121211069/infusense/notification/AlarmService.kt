package com.d121211069.infusense.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.d121211069.infusense.R

class AlarmService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarm"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Peringatan aktif."
                startAlarm(title, message)
            }
            ACTION_STOP -> {
                stopAlarm()
            }
        }
        return START_STICKY
    }

    private fun startAlarm(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peringatan Infus Kritis",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_infus)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(0, "Berhenti", stopPendingIntent)
            .build()

        // Mulai layanan di foreground
        startForeground(NOTIFICATION_ID, notification)

        // Putar suara atau getar
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, alarmUri)
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                ringtone?.play()
            } else {
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(0, 500, 1000, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Gagal memutar alarm: ${e.message}")
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(true)

        // hentukan service sendiri
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    companion object {
        const val ACTION_PLAY = "com.d121211069.infusense.ACTION_PLAY"
        const val ACTION_STOP = "com.d121211069.infusense.ACTION_STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        private const val CHANNEL_ID = "infus_alert_channel_service"
        private const val NOTIFICATION_ID = 1001
    }
}
