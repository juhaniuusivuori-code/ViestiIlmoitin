package com.example.viestiilmoitin

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat

class AppNotificationListener : NotificationListenerService() {

    private var lastWhatsAppTime = 0L
    private var lastMessengerTime = 0L
    private var lastEmailTime = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        val notification = sbn.notification ?: return

        // 1. Ohitetaan taustatoiminnot (kuten puhelut, lataukset) ja ryhmäkoonnit
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        // 2. Ohitetaan hiljaiset ilmoitukset (tapahtuu kun sovellus on auki ruudulla ja kirjoitellaan livenä)
        val ranking = Ranking()
        if (currentRanking != null && currentRanking.getRanking(sbn.key, ranking)) {
            if (ranking.importance < NotificationManager.IMPORTANCE_DEFAULT &&
                ranking.importance != NotificationManager.IMPORTANCE_UNSPECIFIED
            ) {
                // Sovellus on parhaillaan auki ruudulla ja viesti näkyy livenä -> ei huudeta
                return
            }
        }

        val currentTime = System.currentTimeMillis()
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_enabled", true)
        if (!isEnabled) {
            return
        }

        var kuulutusTeksti: String? = null
        var logiNimi: String? = null
        var sovelluksenNimi: String? = null

        // 1. WHATSAPP
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            if (currentTime - lastWhatsAppTime > 3000) {
                lastWhatsAppTime = currentTime
                val baseText = prefs.getString("whatsapp_text", "WHATSUP VIESTI") ?: "WHATSUP VIESTI"
                val lahettaja = etsiViestinLahettaja(notification)

                kuulutusTeksti = if (lahettaja.isNotEmpty()) {
                    "$baseText $lahettaja".trim()
                } else {
                    baseText
                }
                logiNimi = if (lahettaja.isNotEmpty()) lahettaja else "Tuntematon"
                sovelluksenNimi = "WhatsApp"
            }
        }
        // 2. MESSENGER
        else if (pkg == "com.facebook.orca" || pkg == "com.facebook.mlite") {
            if (currentTime - lastMessengerTime > 3000) {
                lastMessengerTime = currentTime
                val baseText = prefs.getString("messenger_text", "MESSENGER VIESTI") ?: "MESSENGER VIESTI"
                val lahettaja = etsiViestinLahettaja(notification)

                kuulutusTeksti = if (lahettaja.isNotEmpty()) {
                    "$baseText $lahettaja".trim()
                } else {
                    baseText
                }
                logiNimi = if (lahettaja.isNotEmpty()) lahettaja else "Tuntematon"
                sovelluksenNimi = "Messenger"
            }
        }
        // 3. SÄHKÖPOSTI (Gmail, Outlook, Samsung Mail, Proton, Yahoo jne.)
        else if (onkoSahkoposti(pkg, notification)) {
            if (currentTime - lastEmailTime > 3000) {
                lastEmailTime = currentTime
                val emailPrefix = prefs.getString("email_text", "SÄHKÖPOSTI") ?: "SÄHKÖPOSTI"
                val lahettaja = etsiSahkopostinLahettaja(notification)

                kuulutusTeksti = if (lahettaja.isNotEmpty()) {
                    "$emailPrefix $lahettaja".trim()
                } else {
                    emailPrefix
                }
                logiNimi = if (lahettaja.isNotEmpty()) lahettaja else "Tuntematon"
                sovelluksenNimi = "Sähköposti"
            }
        }

        if (kuulutusTeksti != null) {
            if (logiNimi != null && sovelluksenNimi != null) {
                LogDatabaseHelper(this).lisaaMerkinta(logiNimi, sovelluksenNimi)
            }

            val repeats = prefs.getInt("repeats", 3)
            val intervalMinutes = prefs.getInt("interval", 1) + 1

            val serviceIntent = Intent(this, AlertService::class.java).apply {
                putExtra("MESSAGE", kuulutusTeksti)
                putExtra("REPEATS_LEFT", repeats)
                putExtra("INTERVAL_MINUTES", intervalMinutes)
                putExtra("SENDER", pkg)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg.contains("whatsapp") || pkg.contains("orca") || pkg.contains("mail") || pkg.contains("gm") || pkg.contains("outlook")) {
            val stopIntent = Intent(this, AlertService::class.java).apply {
                action = "KUITTAA"
            }
            startService(stopIntent)
        }
    }

    private fun etsiViestinLahettaja(notification: Notification): String {
        val extras = notification.extras ?: return ""

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val titleBig = extras.getCharSequence("android.title.big")?.toString()?.trim() ?: ""

        val candidate = when {
            title.isNotEmpty() && !onkoYleisOtsikko(title) -> title
            titleBig.isNotEmpty() && !onkoYleisOtsikko(titleBig) -> titleBig
            else -> title
        }

        return siistiLahettajanNimi(candidate)
    }

    private fun etsiSahkopostinLahettaja(notification: Notification): String {
        val extras = notification.extras ?: return ""

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val titleBig = extras.getCharSequence("android.title.big")?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString()?.trim() ?: ""

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null && lines.isNotEmpty()) {
            val latest = lines.lastOrNull()?.toString()?.trim() ?: ""
            if (latest.isNotEmpty()) {
                val candidate = latest.split(":", "—", "-").firstOrNull()?.trim() ?: ""
                if (candidate.isNotEmpty() && !onkoYleisOtsikko(candidate)) {
                    return siistiLahettajanNimi(candidate)
                }
            }
        }

        val candidate = when {
            title.isNotEmpty() && !onkoYleisOtsikko(title) -> title
            titleBig.isNotEmpty() && !onkoYleisOtsikko(titleBig) -> titleBig
            text.isNotEmpty() && !onkoYleisOtsikko(text) -> {
                if (text.contains(":")) text.substringBefore(":").trim() else text
            }
            bigText.isNotEmpty() && !onkoYleisOtsikko(bigText) -> {
                if (bigText.contains(":")) bigText.substringBefore(":").trim() else bigText
            }
            title.isNotEmpty() -> title
            else -> ""
        }

        return siistiLahettajanNimi(candidate)
    }

    private fun onkoYleisOtsikko(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("uutta viestiä") ||
               lower.contains("uusi viesti") ||
               lower.contains("new message") ||
               lower.contains("unread") ||
               lower.contains("messages")
    }

    private fun siistiLahettajanNimi(raw: String): String {
        var s = raw.trim()
        if (s.contains("<")) {
            s = s.substringBefore("<").trim()
        }
        if (s.contains("(")) {
            s = s.substringBefore("(").trim()
        }
        if (s.isEmpty() && raw.contains("@")) {
            s = raw.substringBefore("@").replace(".", " ").trim()
        }
        return s.ifEmpty { raw }
    }

    private fun onkoSahkoposti(pkg: String, notification: Notification): Boolean {
        val p = pkg.lowercase()
        if (p.contains("gm") ||
            p.contains("outlook") ||
            p.contains("email") ||
            p.contains("mail") ||
            p.contains("k9") ||
            p.contains("thunderbird") ||
            p.contains("proton") ||
            p.contains("yahoo")
        ) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (notification.category == Notification.CATEGORY_EMAIL) {
                return true
            }
        }
        return false
    }
}
