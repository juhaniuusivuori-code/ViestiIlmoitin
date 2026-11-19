package com.example.viestiilmoitin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnMasterSwitch: Button
    private lateinit var editPrefix: EditText
    private lateinit var editUnknown: EditText
    private lateinit var editWhatsApp: EditText
    private lateinit var editMessenger: EditText
    private lateinit var editEmail: EditText
    private lateinit var spinnerRepeats: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var btnNotificationPermission: Button
    private lateinit var tvVolumeLabel: TextView
    private lateinit var seekBarVolume: SeekBar
    private lateinit var btnSettings: Button
    private var tts: TextToSpeech? = null

    private val masterSwitchHandler = Handler(Looper.getMainLooper())
    private var isMasterSwitchLongPressTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMasterSwitch = findViewById(R.id.btnMasterSwitch)
        editPrefix = findViewById(R.id.editPrefix)
        editUnknown = findViewById(R.id.editUnknown)
        editWhatsApp = findViewById(R.id.editWhatsApp)
        editMessenger = findViewById(R.id.editMessenger)
        editEmail = findViewById(R.id.editEmail)
        spinnerRepeats = findViewById(R.id.spinnerRepeats)
        spinnerInterval = findViewById(R.id.spinnerInterval)
        btnNotificationPermission = findViewById(R.id.btnNotificationPermission)
        tvVolumeLabel = findViewById(R.id.tvVolumeLabel)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        btnSettings = findViewById(R.id.btnSettings)

        tts = TextToSpeech(this, this)

        alustaPaaKytkin()
        pyydaLuvat()
        alustaValikot()
        lataaAsetukset()

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val p = progress.coerceAtLeast(10)
                tvVolumeLabel.text = "ÄÄNENVOIMAKKUUS: $p %"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnNotificationPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            tallennaAsetukset()
            Toast.makeText(this, "Asetukset tallennettu!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            soitaTestiAani()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, AsetuksetActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        paivitaIlmoitusLuvanTila()
        val isEnabled = getSharedPreferences("Asetukset", Context.MODE_PRIVATE).getBoolean("app_enabled", true)
        paivitaPaaKytkinNakyma(isEnabled)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun alustaPaaKytkin() {
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)
        var isEnabled = prefs.getBoolean("app_enabled", true)
        paivitaPaaKytkinNakyma(isEnabled)

        val toggleRunnable = Runnable {
            isMasterSwitchLongPressTriggered = true
            isEnabled = !isEnabled
            prefs.edit().putBoolean("app_enabled", isEnabled).apply()
            paivitaPaaKytkinNakyma(isEnabled)

            btnMasterSwitch.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            if (isEnabled) {
                Toast.makeText(this, "Sovellus kytketty PÄÄLLE!", Toast.LENGTH_SHORT).show()
            } else {
                val stopIntent = Intent(this, AlertService::class.java).apply {
                    action = "STOP"
                }
                startService(stopIntent)
                Toast.makeText(this, "Sovellus kytketty POIS PÄÄLTÄ!", Toast.LENGTH_SHORT).show()
            }
        }

        btnMasterSwitch.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMasterSwitchLongPressTriggered = false
                    btnMasterSwitch.text = "Pidetään pohjassa... (1 s)"
                    btnMasterSwitch.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.parseColor("#E65100"))
                    masterSwitchHandler.postDelayed(toggleRunnable, 1000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    masterSwitchHandler.removeCallbacks(toggleRunnable)
                    paivitaPaaKytkinNakyma(isEnabled)
                    if (!isMasterSwitchLongPressTriggered && event.action == MotionEvent.ACTION_UP) {
                        Toast.makeText(this, "Pidä nappia pohjassa 1 sekunti vaihtaaksesi tilan", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun paivitaPaaKytkinNakyma(isEnabled: Boolean) {
        if (isEnabled) {
            btnMasterSwitch.text = "SOVELLUS ON PÄÄLLÄ"
            btnMasterSwitch.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))
        } else {
            btnMasterSwitch.text = "SOVELLUS ON POIS PÄÄLTÄ"
            btnMasterSwitch.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.parseColor("#C62828"))
        }
    }

    private fun paivitaIlmoitusLuvanTila() {
        val onkoLupa = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (onkoLupa) {
            btnNotificationPermission.text = "ILMOITUSKUUNTELU ON PÄÄLLÄ\n(WhatsApp, Messenger ja Sähköposti aktivoitu)"
            btnNotificationPermission.setBackgroundColor(android.graphics.Color.parseColor("#388E3C"))
        } else {
            btnNotificationPermission.text = "SALLI ILMOITUKSET\n(Paina tästä ja valitse: Viesti-Ilmoitin)"
            btnNotificationPermission.setBackgroundColor(android.graphics.Color.parseColor("#FF8F00"))
        }
    }

    private fun alustaValikot() {
        val repeatOptions = arrayOf("Ei toistoa", "1 kerta", "2 kertaa", "3 kertaa", "4 kertaa", "5 kertaa", "6 kertaa", "7 kertaa", "8 kertaa", "9 kertaa", "10 kertaa")
        val intervalOptions = (1..10).map { "$it min välein" }.toTypedArray()

        spinnerRepeats.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeatOptions)
        spinnerInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervalOptions)
    }

    private fun lataaAsetukset() {
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)
        editPrefix.setText(prefs.getString("prefix", "VIESTI TULI "))
        editUnknown.setText(prefs.getString("unknown", "VIESTI NUMEROSTA "))
        editWhatsApp.setText(prefs.getString("whatsapp_text", "WHATSUP VIESTI"))
        editMessenger.setText(prefs.getString("messenger_text", "MESSENGER VIESTI"))
        editEmail.setText(prefs.getString("email_text", "SÄHKÖPOSTI"))
        spinnerRepeats.setSelection(prefs.getInt("repeats", 3))
        spinnerInterval.setSelection(prefs.getInt("interval", 1))

        val vol = prefs.getInt("volume_percent", 100).coerceAtLeast(10)
        seekBarVolume.progress = vol
        tvVolumeLabel.text = "ÄÄNENVOIMAKKUUS: $vol %"
    }

    private fun tallennaAsetukset() {
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE).edit()
        prefs.putString("prefix", editPrefix.text.toString())
        prefs.putString("unknown", editUnknown.text.toString())
        prefs.putString("whatsapp_text", editWhatsApp.text.toString())
        prefs.putString("messenger_text", editMessenger.text.toString())
        prefs.putString("email_text", editEmail.text.toString())
        prefs.putInt("repeats", spinnerRepeats.selectedItemPosition)
        prefs.putInt("interval", spinnerInterval.selectedItemPosition)

        val vol = seekBarVolume.progress.coerceAtLeast(10)
        prefs.putInt("volume_percent", vol)
        prefs.apply()
    }

    private var alkuperainenHalytysAani: Int? = null

    private fun soitaTestiAani() {
        val volPercent = (seekBarVolume.progress.coerceIn(10, 100)) / 100f
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (alkuperainenHalytysAani == null) {
                alkuperainenHalytysAani = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            }
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVolume = (maxVolume * volPercent).toInt().coerceIn(1, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        } catch (e: Exception) {
        }

        val testiLause = "${editPrefix.text} Pekka Virtanen. ${editWhatsApp.text} Peksu. ${editMessenger.text} Matti Meikäläinen. ${editEmail.text} Liisa.".trim()
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(testiLause, TextToSpeech.QUEUE_FLUSH, params, "TEST_ID")
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

    private fun pyydaLuvat() {
        val luvat = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            luvat.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, luvat.toTypedArray(), 100)
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
                    masterSwitchHandler.post { palautaAlkuperainenHalytysAani() }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    masterSwitchHandler.post { palautaAlkuperainenHalytysAani() }
                }
            })
        }
    }

    override fun onDestroy() {
        palautaAlkuperainenHalytysAani()
        tts?.shutdown()
        super.onDestroy()
    }
}
