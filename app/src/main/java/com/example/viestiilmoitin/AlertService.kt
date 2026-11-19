package com.example.viestiilmoitin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

class AlertService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var viestiTeksti: String = ""
    private var repeatsLeft: Int = 0
    private var intervalMinutes: Int = 1
    private var lahettaja: String = ""
    private var onTekstiviesti: Boolean = false
    private var ttsYrityksia = 0
    private var lopetusOdottaa = false
    private var alkuperainenHalytysAani: Int? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        luoIlmoitusKanava()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android vaatii startForeground-kutsun aina, kun palvelu käynnistetään startForegroundService-kutsulla
        naytaKuittausIlmoitus(intent?.getStringExtra("MESSAGE") ?: "")

        if (intent?.action == "KUITTAA" || intent?.action == "STOP") {
            lopetaHalytys()
            return Service.START_NOT_STICKY
        }

        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("app_enabled", true)) {
            lopetaHalytys()
            return Service.START_NOT_STICKY
        }

        // Uusi viesti korvaa edellisen: nollataan vanhat ajastukset ettei toistoja tule päällekkäin
        handler.removeCallbacksAndMessages(null)
        lopetusOdottaa = false
        ttsYrityksia = 0

        viestiTeksti = intent?.getStringExtra("MESSAGE") ?: ""
        repeatsLeft = intent?.getIntExtra("REPEATS_LEFT", 0) ?: 0
        intervalMinutes = intent?.getIntExtra("INTERVAL_MINUTES", 1) ?: 1
        lahettaja = intent?.getStringExtra("SENDER") ?: ""
        onTekstiviesti = intent?.getBooleanExtra("IS_SMS", false) ?: false

        // Odotetaan 2,3 sekuntia (1 - 1,5 s lisäviive), jotta puhelimen oma viestiääni ("pling")
        // ehtii soida loppuun ennen kuin puhe alkaa
        handler.postDelayed({ puhuLujallaAanella() }, 2300L)

        return Service.START_NOT_STICKY
    }

    private fun puhuLujallaAanella() {
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("app_enabled", true)) {
            lopetaHalytys()
            return
        }

        if (onkoViestiLuettu()) {
            lopetaHalytys()
            return
        }

        // Jos puhesyntetisaattori ei ole vielä valmis, yritetään uudelleen hetken päästä
        if (!isTtsReady) {
            ttsYrityksia++
            if (ttsYrityksia <= 10) {
                handler.postDelayed({ puhuLujallaAanella() }, 500)
            } else {
                lopetaHalytys()
            }
            return
        }

        val volumePercent = (prefs.getInt("volume_percent", 100).coerceIn(10, 100)) / 100f

        // Käytetään erillistä STREAM_ALARM-kanavaa eikä kosketa lainkaan STREAM_MUSIC-kanavaan,
        // jotta muiden sovellusten (esim. TikTok, YouTube, radio) äänenvoimakkuus ei muutu.
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (alkuperainenHalytysAani == null) {
                alkuperainenHalytysAani = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            }
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVol = (maxVol * volumePercent).toInt().coerceIn(1, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
        } catch (e: Exception) {
        }

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(viestiTeksti, TextToSpeech.QUEUE_FLUSH, params, "ALERT_ID")

        if (repeatsLeft > 0) {
            repeatsLeft--
            handler.postDelayed({ puhuLujallaAanella() }, intervalMinutes * 60 * 1000L)
        } else {
            // Viimeinen kuulutus: odotetaan että puhe ehtii loppua ennen palvelun sulkemista
            lopetusOdottaa = true
            handler.postDelayed({ lopetaHalytys() }, 20000)
        }
    }

    private fun palautaAlkuperainenHalytysAani() {
        val palautettava = alkuperainenHalytysAani ?: return
        alkuperainenHalytysAani = null
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, palautettava, 0)
        } catch (e: Exception) {
        }
    }

    private fun onkoViestiLuettu(): Boolean {
        // Sovellusten (WhatsApp, Messenger, Sähköposti) ilmoitukset kuittaantuvat
        // kun käyttäjä avaa viestin tai pyyhkii sen pois (onNotificationRemoved)
        if (!onTekstiviesti) {
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

    private fun naytaKuittausIlmoitus(teksti: String) {
        val notification = NotificationCompat.Builder(this, "HalytysKanava")
            .setContentTitle("Saapui viesti!")
            .setContentText(teksti)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)
    }

    private fun lopetaHalytys() {
        handler.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
        } catch (e: Exception) {
        }
        palautaAlkuperainenHalytysAani()
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
            val kanava = NotificationChannel("HalytysKanava", "Viestihälytykset", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(kanava)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("fi", "FI")
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    handler.post {
                        palautaAlkuperainenHalytysAani()
                        if (lopetusOdottaa) {
                            lopetaHalytys()
                        }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handler.post {
                        palautaAlkuperainenHalytysAani()
                        if (lopetusOdottaa) {
                            lopetaHalytys()
                        }
                    }
                }
            })
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        palautaAlkuperainenHalytysAani()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
