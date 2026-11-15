package com.example.viestiilmoitin

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var editPrefix: EditText
    private lateinit var editSuffix: EditText
    private lateinit var editUnknown: EditText
    private lateinit var spinnerRepeats: Spinner
    private lateinit var spinnerInterval: Spinner
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editPrefix = findViewById(R.id.editPrefix)
        editSuffix = findViewById(R.id.editSuffix)
        editUnknown = findViewById(R.id.editUnknown)
        spinnerRepeats = findViewById(R.id.spinnerRepeats)
        spinnerInterval = findViewById(R.id.spinnerInterval)

        tts = TextToSpeech(this, this)

        pyydaLuvat()
        alustaValikot()
        lataaAsetukset()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            tallennaAsetukset()
            Toast.makeText(this, "Asetukset tallennettu!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            soitaTestiAani()
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
        spinnerRepeats.setSelection(prefs.getInt("repeats", 3))
        spinnerInterval.setSelection(prefs.getInt("interval", 1))
    }

    private fun tallennaAsetukset() {
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE).edit()
        prefs.putString("prefix", editPrefix.text.toString())
        prefs.putString("suffix", editSuffix.text.toString())
        prefs.putString("unknown", editUnknown.text.toString())
        prefs.putInt("repeats", spinnerRepeats.selectedItemPosition)
        prefs.putInt("interval", spinnerInterval.selectedItemPosition)
        prefs.apply()
    }

    private fun soitaTestiAani() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        val testiLause = "${editPrefix.text} Pekka Virtanen ${editSuffix.text}".trim()
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
