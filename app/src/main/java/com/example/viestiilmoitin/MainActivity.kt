package com.example.viestiilmoitin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var editPrefix: EditText
    private lateinit var editSuffix: EditText
    private lateinit var editUnknown: EditText
    private lateinit var editWhatsApp: EditText
    private lateinit var editMessenger: EditText
    private lateinit var spinnerRepeats: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var btnNotificationPermission: Button
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editPrefix = findViewById(R.id.editPrefix)
        editSuffix = findViewById(R.id.editSuffix)
        editUnknown = findViewById(R.id.editUnknown)
        editWhatsApp = findViewById(R.id.editWhatsApp)
        editMessenger = findViewById(R.id.editMessenger)
        spinnerRepeats = findViewById(R.id.spinnerRepeats)
        spinnerInterval = findViewById(R.id.spinnerInterval)
        btnNotificationPermission = findViewById(R.id.btnNotificationPermission)

        tts = TextToSpeech(this, this)

        pyydaLuvat()
        alustaValikot()
        lataaAsetukset()

        btnNotificationPermission.setOnClickListener {
            // Avataan suoraan Androidin asetusruutu, josta kytketään ilmoituslupa päälle
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
    }

    override fun onResume() {
        super.onResume()
        paivitaIlmoitusLuvanTila()
    }

    private fun paivitaIlmoitusLuvanTila() {
        val onkoLupa = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (onkoLupa) {
            btnNotificationPermission.text = "✅ ILMOITUSKUUNTELU ON PÄÄLLÄ\n(WhatsApp ja Messenger aktivoitu)"
            btnNotificationPermission.setBackgroundColor(android.graphics.Color.parseColor("#388E3C"))
        } else {
            btnNotificationPermission.text = "🔔 SALLI ILMOITUKSET\n(Paina tästä ja valitse listasta: Viesti-Ilmoitin)"
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
        editSuffix.setText(prefs.getString("suffix", ""))
        editUnknown.setText(prefs.getString("unknown", "VIESTI NUMEROSTA "))
        editWhatsApp.setText(prefs.getString("whatsapp_text", "WHATSUP VIESTI"))
        editMessenger.setText(prefs.getString("messenger_text", "MESSENGER VIESTI"))
        spinnerRepeats.setSelection(prefs.getInt("repeats", 3))
        spinnerInterval.setSelection(prefs.getInt("interval", 1))
    }

    private fun tallennaAsetukset() {
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE).edit()
        prefs.putString("prefix", editPrefix.text.toString())
        prefs.putString("suffix", editSuffix.text.toString())
        prefs.putString("unknown", editUnknown.text.toString())
        prefs.putString("whatsapp_text", editWhatsApp.text.toString())
        prefs.putString("messenger_text", editMessenger.text.toString())
        prefs.putInt("repeats", spinnerRepeats.selectedItemPosition)
        prefs.putInt("interval", spinnerInterval.selectedItemPosition)
        prefs.apply()
    }

    private fun soitaTestiAani() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        val testiLause = "${editPrefix.text} Pekka Virtanen. ${editWhatsApp.text}!".trim()
        tts?.speak(testiLause, TextToSpeech.QUEUE_FLUSH, null, "TEST_ID")
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
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
