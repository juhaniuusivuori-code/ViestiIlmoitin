package com.example.viestiilmoitin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
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
    private lateinit var editSuffix: EditText
    private lateinit var editUnknown: EditText
    private lateinit var editWhatsApp: EditText
    private lateinit var editMessenger: EditText
    private lateinit var editEmail: EditText
    private lateinit var spinnerRepeats: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var btnNotificationPermission: Button
    private lateinit var tvVolumeLabel: TextView
    private lateinit var seekBarVolume: SeekBar
    private lateinit var btnHelp: Button
    private lateinit var btnLog: Button
    private var tts: TextToSpeech? = null

    private val masterSwitchHandler = Handler(Looper.getMainLooper())
    private var isMasterSwitchLongPressTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMasterSwitch = findViewById(R.id.btnMasterSwitch)
        editPrefix = findViewById(R.id.editPrefix)
        editSuffix = findViewById(R.id.editSuffix)
        editUnknown = findViewById(R.id.editUnknown)
        editWhatsApp = findViewById(R.id.editWhatsApp)
        editMessenger = findViewById(R.id.editMessenger)
        editEmail = findViewById(R.id.editEmail)
        spinnerRepeats = findViewById(R.id.spinnerRepeats)
        spinnerInterval = findViewById(R.id.spinnerInterval)
        btnNotificationPermission = findViewById(R.id.btnNotificationPermission)
        tvVolumeLabel = findViewById(R.id.tvVolumeLabel)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        btnHelp = findViewById(R.id.btnHelp)
        btnLog = findViewById(R.id.btnLog)

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

        btnHelp.setOnClickListener {
            naytaOhjeet()
        }

        btnLog.setOnClickListener {
            startActivity(Intent(this, LokiActivity::class.java))
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
        editSuffix.setText(prefs.getString("suffix", ""))
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
        prefs.putString("suffix", editSuffix.text.toString())
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

    private fun soitaTestiAani() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volPercent = seekBarVolume.progress.coerceAtLeast(10) / 100f
        val targetVolume = (maxVolume * volPercent).toInt().coerceIn(1, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)

        val testiLause = "${editPrefix.text} Pekka Virtanen. ${editWhatsApp.text} Peksu. ${editMessenger.text} Matti Meikäläinen. ${editEmail.text} Liisa.".trim()
        tts?.speak(testiLause, TextToSpeech.QUEUE_FLUSH, null, "TEST_ID")
    }

    private fun naytaOhjeet() {
        val ohjeTeksti = """
            Tervetuloa käyttämään Viesti-Ilmoitinta!
            Tämä sovellus ilmoittaa saapuvista tekstiviesteistä, WhatsApp-, Messenger- ja sähköpostiviesteistä suurella äänellä.

            ━━━━━━━━━━━━━━━━━━━━━━
            1. PÄÄLLÄ / POIS -PÄÄKYTKIN
            ━━━━━━━━━━━━━━━━━━━━━━
            • Ruudun ylimmäisenä on pääkytkin, josta saat koko sovelluksen ilmoitukset päälle tai pois päältä.
            • Painikkeessa on 1 sekunnin turvaviive vahinkopainallusten estämiseksi: pidä painiketta yhtäjaksoisesti pohjassa yhden sekunnin ajan vaihtaaksesi tilan.
            • Kun kytkin on VIHREÄ (PÄÄLLÄ), sovellus kuuntelee ja huutaa saapuvat viestit.
            • Kun kytkin on PUNAINEN (POIS PÄÄLTÄ), sovellus on täysin hiljainen eikä mitään ilmoituksia huudeta.

            ━━━━━━━━━━━━━━━━━━━━━━
            2. TÄRKEÄT LUVAT (ASETUKSET)
            ━━━━━━━━━━━━━━━━━━━━━━
            • TEKSTIVIESTIT JA YHTEYSTIEDOT:
              Puhelin kysyy näitä lupia automaattisesti. Valitse aina "Salli".
              (Tarvitaan, jotta sovellus havaitsee saapuvan viestin ja löytää nimen puhelimen osoitekirjasta).

            • WHATSAPP, MESSENGER JA SÄHKÖPOSTI:
              1. Paina päänäytön oranssia painiketta "SALLI ILMOITUKSET".
              2. Puhelin siirtyy laitteen asetuksiin. Etsi listalta "Viesti-Ilmoitin".
              3. Napauta sitä ja kytke lupa päälle (Salli).
              4. Kun palaat sovellukseen, painike muuttuu vihreäksi.

            * VINKKI (jos puhelin ilmoittaa 'Rajoitettu asetus'):
              Mene: Puhelimen Asetukset ➔ Sovellukset ➔ Viesti-Ilmoitin ➔ Paina oikeasta yläkulmasta 3 pistettä ➔ Valitse "Salli rajoitetut asetukset". Tämän jälkeen luvan saa kytkettyä päälle.

            ━━━━━━━━━━━━━━━━━━━━━━
            3. VIESTIEN HUUTOLAUSEET JA LÄHETTÄJÄT
            ━━━━━━━━━━━━━━━━━━━━━━
            • TEKSTIVIESTI:
              - Ennen nimeä: Esim. "VIESTI TULI "
              - Nimen jälkeen: Esim. " LAITTOI VIESTIN" (tai jätä tyhjäksi).
              - Tuntematon numero: Mitä huudetaan, jos numeroa ei löydy osoitekirjasta (esim. "VIESTI NUMEROSTA ").
              (Voit muuttaa henkilön nimen tai lempinimen suoraan puhelimen omaan osoitekirjaan).

            • WHATSAPP JA MESSENGER:
              - Sovellus ilmoittaa aina saapuneen viestin lähettäjän (esim. "WHATSUP VIESTI Peksu" tai "MESSENGER VIESTI Matti").
              - HUOMIO: Jos olet parhaillaan kirjoittelemassa kyseisessä sovelluksessa (sovellus on auki ruudulla), puhelin ei huuda ilmoitusta, koska näet viestin itse livenä.

            • SÄHKÖPOSTI:
              - Sovellus tunnistaa saapuneen sähköpostin ja huutaa lähettäjän nimen (esim. "SÄHKÖPOSTI Matti Meikäläinen").

            ━━━━━━━━━━━━━━━━━━━━━━
            4. TOISTO JA KUITTAUS
            ━━━━━━━━━━━━━━━━━━━━━━
            • TOISTOKERRAT JA -VÄLI:
              Valitse, kuinka monta kertaa (1-10) ja kuinka usein (1-10 min) ilmoitus toistetaan, jos viestiä ei heti huomata.
            • MITEN TOISTO SAMMUTETAAN:
              Viestin toistohälytys kuittaantuu ja sammuu automaattisesti, kun viesti tai sähköposti avataan tai pyyhkäistään pois.

            ━━━━━━━━━━━━━━━━━━━━━━
            5. ÄÄNENVOIMAKKUUDEN SÄÄTÖ
            ━━━━━━━━━━━━━━━━━━━━━━
            • Ruudun alaosassa olevalla liukusäätimellä voit määrittää äänenvoimakkuuden (10 % – 100 %).
            • Kokeile äänenvoimakkuutta painamalla "KOKEILE ÄÄNTÄ".

            ━━━━━━━━━━━━━━━━━━━━━━
            6. VIESTILOKI
            ━━━━━━━━━━━━━━━━━━━━━━
            • Paina päänäytöltä "LOKI"-painiketta.
            • Lokista näet kaikki viestien lähettäjät, kuinka monta viestiä kukin on lähettänyt yhteensä ja minä päivinä.
            • Lokin voi tyhjentää pitämällä "NOLLAA LOKI" -painiketta pohjassa yhden sekunnin ajan (estää vahinkopainallukset).

            ━━━━━━━━━━━━━━━━━━━━━━
            7. TALLENNUS JA KOKEILU
            ━━━━━━━━━━━━━━━━━━━━━━
            • Aivan ruudun alimmaisena on suuri sininen painike: "TALLENNA ASETUKSET".
            • Paina "KOKEILE ÄÄNTÄ" testataksesi ääntä kaiuttimesta ja muista aina lopuksi painaa alhaalta "TALLENNA ASETUKSET"!
        """.trimIndent()

        val scrollView = ScrollView(this).apply {
            setPadding(32, 24, 32, 24)
        }
        val textView = TextView(this).apply {
            text = ohjeTeksti
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#212121"))
            setLineSpacing(4f, 1.15f)
        }
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("OHJE JA KÄYTTÖOPAS")
            .setView(scrollView)
            .setPositiveButton("SULJE OHJE") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
