package com.example.audiotest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AudioSynthesizerService : Service() {

    companion object {
        const val CHANNEL_ID = "audio_synthesizer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.audiotest.ACTION_START"
        const val ACTION_STOP = "com.example.audiotest.ACTION_STOP"
    }

    private val binder = LocalBinder()
    private var engine: NativeSynthesizerEngine? = null

    var onFrequencyListener: ((Float) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): AudioSynthesizerService = this@AudioSynthesizerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        engine = NativeSynthesizerEngine { currentFreq ->
            onFrequencyListener?.invoke(currentFreq)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSynthesizer()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startSynthesizer()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startSynthesizer() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (engine?.isSynthesizing == false) {
            engine?.start()
        }
    }

    fun stopSynthesizer() {
        engine?.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    val isSynthesizing: Boolean
        get() = engine?.isSynthesizing == true

    var centerFrequency: Float
        get() = engine?.centerFrequency ?: 440f
        set(value) {
            engine?.centerFrequency = value
        }

    var modulationDepth: Float
        get() = engine?.modulationDepth ?: 150f
        set(value) {
            engine?.modulationDepth = value
        }

    var modulationRate: Float
        get() = engine?.modulationRate ?: 1f
        set(value) {
            engine?.modulationRate = value
        }

    var volume: Float
        get() = engine?.volume ?: 0.5f
        set(value) {
            engine?.volume = value
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.notification_channel_name)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setSound(null, null)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioSynthesizerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_synth_notification)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        stopSynthesizer()
        engine?.destroy()
        engine = null
        super.onDestroy()
    }
}
