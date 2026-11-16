package com.example.viestiilmoitin

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AsetuksetActivity : AppCompatActivity() {

    private lateinit var btnToggleWhatsApp: Button
    private lateinit var btnToggleMessenger: Button
    private lateinit var btnToggleSms: Button
    private lateinit var btnToggleEmail: Button
    private lateinit var btnHelp: Button
    private lateinit var btnLog: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asetukset)

        btnToggleWhatsApp = findViewById(R.id.btnToggleWhatsApp)
        btnToggleMessenger = findViewById(R.id.btnToggleMessenger)
        btnToggleSms = findViewById(R.id.btnToggleSms)
        btnToggleEmail = findViewById(R.id.btnToggleEmail)
        btnHelp = findViewById(R.id.btnHelp)
        btnLog = findViewById(R.id.btnLog)
        btnBack = findViewById(R.id.btnBack)

        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)

        paivitaKytkin(btnToggleWhatsApp, prefs.getBoolean("enabled_whatsapp", true))
        paivitaKytkin(btnToggleMessenger, prefs.getBoolean("enabled_messenger", true))
        paivitaKytkin(btnToggleSms, prefs.getBoolean("enabled_sms", true))
        paivitaKytkin(btnToggleEmail, prefs.getBoolean("enabled_email", true))

        btnToggleWhatsApp.setOnClickListener {
            val uusiTila = !prefs.getBoolean("enabled_whatsapp", true)
            prefs.edit().putBoolean("enabled_whatsapp", uusiTila).apply()
            paivitaKytkin(btnToggleWhatsApp, uusiTila)
        }

        btnToggleMessenger.setOnClickListener {
            val uusiTila = !prefs.getBoolean("enabled_messenger", true)
            prefs.edit().putBoolean("enabled_messenger", uusiTila).apply()
            paivitaKytkin(btnToggleMessenger, uusiTila)
        }

        btnToggleSms.setOnClickListener {
            val uusiTila = !prefs.getBoolean("enabled_sms", true)
            prefs.edit().putBoolean("enabled_sms", uusiTila).apply()
            paivitaKytkin(btnToggleSms, uusiTila)
        }

        btnToggleEmail.setOnClickListener {
            val uusiTila = !prefs.getBoolean("enabled_email", true)
            prefs.edit().putBoolean("enabled_email", uusiTila).apply()
            paivitaKytkin(btnToggleEmail, uusiTila)
        }

        btnHelp.setOnClickListener {
            naytaOhjeet()
        }

        btnLog.setOnClickListener {
            startActivity(Intent(this, LokiActivity::class.java))
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun paivitaKytkin(button: Button, isEnabled: Boolean) {
        if (isEnabled) {
            button.text = "PÄÄLLÄ"
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
        } else {
            button.text = "POIS"
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C62828"))
        }
    }

    private fun naytaOhjeet() {
        val ohjeTeksti = """
            Tervetuloa käyttämään Viesti-Ilmoitinta!
            Tämä sovellus ilmoittaa saapuvista tekstiviesteistä, WhatsApp-, Messenger- ja sähköpostiviesteistä suurella äänellä.

            ━━━━━━━━━━━━━━━━━━━━━━
            1. PÄÄLLÄ / POIS -PÄÄKYTKIN
            ━━━━━━━━━━━━━━━━━━━━━━
            • Etusivun ylimmäisenä on pääkytkin, josta saat koko sovelluksen ilmoitukset päälle tai pois päältä.
            • Painikkeessa on 1 sekunnin turvaviive vahinkopainallusten estämiseksi: pidä painiketta yhtäjaksoisesti pohjassa yhden sekunnin ajan vaihtaaksesi tilan.
            • Kun kytkin on VIHREÄ (SOVELLUS ON PÄÄLLÄ), sovellus kuuntelee ja huutaa saapuvat viestit.
            • Kun kytkin on PUNAINEN (SOVELLUS ON POIS PÄÄLTÄ), sovellus on täysin hiljainen eikä mitään ilmoituksia huudeta.

            ━━━━━━━━━━━━━━━━━━━━━━
            2. ASETUKSET-SIVU JA OHJELMIEN VALINTA
            ━━━━━━━━━━━━━━━━━━━━━━
            • Etusivun "ASETUKSET"-painikkeesta aukeaa tämä sivu, josta voit valita erikseen jokaisen viestityypin (WHATSUP, MESSENGER, SMS, S-POSTI) kohdalta PÄÄLLÄ tai POIS.
            • Jos jonkin ohjelman kohdalla on valittuna POIS (punainen), sovellus ei kuuntele eikä ilmoita kyseisen ohjelman viestejä.
            • Samalta Asetukset-sivulta löytyvät myös tämä OHJE-painike sekä LOKI-painike.

            ━━━━━━━━━━━━━━━━━━━━━━
            3. TÄRKEÄT LUVAT
            ━━━━━━━━━━━━━━━━━━━━━━
            • TEKSTIVIESTIT JA YHTEYSTIEDOT:
              Puhelin kysyy näitä lupia automaattisesti. Valitse aina "Salli".
              (Tarvitaan, jotta sovellus havaitsee saapuvan viestin ja löytää nimen puhelimen osoitekirjasta).

            • WHATSAPP, MESSENGER JA SÄHKÖPOSTI:
              1. Paina etusivun oranssia painiketta "SALLI ILMOITUKSET".
              2. Puhelin siirtyy laitteen asetuksiin. Etsi listalta "Viesti-Ilmoitin".
              3. Napauta sitä ja kytke lupa päälle (Salli).
              4. Kun palaat sovellukseen, painike muuttuu vihreäksi.

            * VINKKI (jos puhelin ilmoittaa 'Rajoitettu asetus'):
              Mene: Puhelimen Asetukset ➔ Sovellukset ➔ Viesti-Ilmoitin ➔ Paina oikeasta yläkulmasta 3 pistettä ➔ Valitse "Salli rajoitetut asetukset". Tämän jälkeen luvan saa kytkettyä päälle.

            ━━━━━━━━━━━━━━━━━━━━━━
            4. VIESTIEN HUUTOLAUSEET JA LÄHETTÄJÄT
            ━━━━━━━━━━━━━━━━━━━━━━
            • TEKSTIVIESTI (SMS):
              - SMS-huuto: Esim. "VIESTI TULI" (sovellus sanoo tämän ja lähettäjän nimen).
              - SMS tuntematon-huuto: Mitä huudetaan, jos numeroa ei löydy osoitekirjasta (esim. "VIESTI NUMEROSTA").
              (Voit muuttaa henkilön nimen tai lempinimen suoraan puhelimen omaan osoitekirjaan).

            • WHATSAPP JA MESSENGER:
              - Sovellus ilmoittaa aina saapuneen viestin lähettäjän (esim. "WHATSUP VIESTI Peksu" tai "MESSENGER VIESTI Matti").
              - HUOMIO: Jos olet parhaillaan kirjoittelemassa kyseisessä sovelluksessa (sovellus on auki ruudulla), puhelin ei huuda ilmoitusta, koska näet viestin itse livenä.

            • SÄHKÖPOSTI:
              - Sovellus tunnistaa saapuneen sähköpostin ja huutaa lähettäjän nimen (esim. "SÄHKÖPOSTI Matti Meikäläinen").

            ━━━━━━━━━━━━━━━━━━━━━━
            5. TOISTO JA KUITTAUS
            ━━━━━━━━━━━━━━━━━━━━━━
            • TOISTOKERRAT JA -VÄLI:
              Valitse, kuinka monta kertaa (1-10) ja kuinka usein (1-10 min) ilmoitus toistetaan, jos viestiä ei heti huomata.
            • MITEN TOISTO SAMMUTETAAN:
              Viestin toistohälytys kuittaantuu ja sammuu automaattisesti, kun viesti tai sähköposti avataan tai pyyhkäistään pois.

            ━━━━━━━━━━━━━━━━━━━━━━
            6. ÄÄNENVOIMAKKUUS JA TALLENNUS
            ━━━━━━━━━━━━━━━━━━━━━━
            • Etusivun alaosassa olevalla liukusäätimellä voit määrittää äänenvoimakkuuden (10 % – 100 %).
            • Kokeile äänenvoimakkuutta painamalla "KOKEILE ÄÄNTÄ".
            • Aivan etusivun alimmaisena on suuri sininen painike: "TALLENNA ASETUKSET". Muista aina lopuksi painaa sitä!

            ━━━━━━━━━━━━━━━━━━━━━━
            7. VIESTILOKI
            ━━━━━━━━━━━━━━━━━━━━━━
            • Paina Asetukset-sivulta "LOKI"-painiketta.
            • Lokista näet kaikki viestien lähettäjät, kuinka monta viestiä kukin on lähettänyt yhteensä ja minä päivinä.
            • Lokin voi tyhjentää pitämällä "NOLLAA LOKI" -painiketta pohjassa yhden sekunnin ajan (estää vahinkopainallukset).
        """.trimIndent()

        val scrollView = ScrollView(this).apply {
            setPadding(32, 24, 32, 24)
        }
        val textView = TextView(this).apply {
            text = ohjeTeksti
            textSize = 15f
            setTextColor(Color.parseColor("#212121"))
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
}
