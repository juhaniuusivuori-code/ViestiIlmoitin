package com.example.viestiilmoitin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class AlertService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var viestiTeksti: String = ""
    private var repeatsLeft: Int = 0
    private var intervalMinutes: Int = 1
    private var lahettaja: String = ""

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        luoIlmoitusKanava()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "KUITTAA") {
            lopetaHalytys()
            return Service.START_NOT_STICKY
        }

        viestiTeksti = intent?.getStringExtra("MESSAGE") ?: ""
        repeatsLeft = intent?.getIntExtra("REPEATS_LEFT", 0) ?: 0
        intervalMinutes = intent?.getIntExtra("INTERVAL_MINUTES", 1) ?: 1
        lahettaja = intent?.getStringExtra("SENDER") ?: ""

        naytaKuittausIlmoitus()
        handler.postDelayed({ puhuLujallaAanella() }, 1000)

        return Service.START_NOT_STICKY
    }

    private fun puhuLujallaAanella() {
        if (onkoViestiLuettu()) {
            lopetaHalytys()
            return
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

        if (isTtsReady) {
            tts?.speak(viestiTeksti, TextToSpeech.QUEUE_FLUSH, null, "ALERT_ID")
        }

        if (repeatsLeft > 0) {
            repeatsLeft--
            handler.postDelayed({ puhuLujallaAanella() }, intervalMinutes * 60 * 1000L)
        } else {
            lopetaHalytys()
        }
    }

    private fun onkoViestiLuettu(): Boolean {
        // Sovellusten (WhatsApp, Messenger, Sähköposti) ilmoitukset kuittaantuvat
        // kun käyttäjä avaa viestin tai pyyhkii sen pois (onNotificationRemoved)
        if (lahettaja.startsWith("com.") || lahettaja.startsWith("net.")) {
            return false
        }

        return try {
            val uri = Uri.parse("content://sms/inbox")
            val cursor = contentResolver.query(uri, arrayOf("read"), "address = ? AND read = 0", arrayOf(lahettaja), null)
            val lukemattomia = cursor?.count ?: 0
            cursor?.close()
            lukemattomia == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun naytaKuittausIlmoitus() {
        val notification = NotificationCompat.Builder(this, "HalytysKanava")
            .setContentTitle("Saapui viesti!")
            .setContentText(viestiTeksti)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)
    }

    private fun lopetaHalytys() {
        handler.removeCallbacksAndMessages(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun luoIlmoitusKanava() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanava = NotificationChannel("HalytysKanava", "Viestihälytykset", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(kanava)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("fi", "FI")
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
