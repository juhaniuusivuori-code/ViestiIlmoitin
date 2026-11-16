package com.example.viestiilmoitin

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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

        // Ohitetaan taustatoiminnot (kuten puhelut tai lataukset) ja ryhmäkoonnit
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val currentTime = System.currentTimeMillis()
        val prefs = getSharedPreferences("Asetukset", Context.MODE_PRIVATE)

        var kuulutusTeksti: String? = null
        var logiNimi: String? = null
        var sovelluksenNimi: String? = null

        val rawTitle = notification.extras.getString(Notification.EXTRA_TITLE)?.trim() ?: ""

        // 1. WHATSAPP
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            if (currentTime - lastWhatsAppTime > 3000) {
                lastWhatsAppTime = currentTime
                val baseText = prefs.getString("whatsapp_text", "WHATSUP VIESTI") ?: "WHATSUP VIESTI"
                val loytyyLuettelosta = onkoYhteystiedoissa(this, rawTitle)

                kuulutusTeksti = if (loytyyLuettelosta && rawTitle.isNotEmpty()) {
                    "$baseText $rawTitle".trim()
                } else {
                    baseText
                }
                logiNimi = if (rawTitle.isNotEmpty()) rawTitle else "Tuntematon"
                sovelluksenNimi = "WhatsApp"
            }
        }
        // 2. MESSENGER
        else if (pkg == "com.facebook.orca" || pkg == "com.facebook.mlite") {
            if (currentTime - lastMessengerTime > 3000) {
                lastMessengerTime = currentTime
                val baseText = prefs.getString("messenger_text", "MESSENGER VIESTI") ?: "MESSENGER VIESTI"
                val loytyyLuettelosta = onkoYhteystiedoissa(this, rawTitle)

                kuulutusTeksti = if (loytyyLuettelosta && rawTitle.isNotEmpty()) {
                    "$baseText $rawTitle".trim()
                } else {
                    baseText
                }
                logiNimi = if (rawTitle.isNotEmpty()) rawTitle else "Tuntematon"
                sovelluksenNimi = "Messenger"
            }
        }
        // 3. SÄHKÖPOSTI (Gmail, Outlook, Samsung Mail, K-9 jne.)
        else if (onkoSahkoposti(pkg, notification)) {
            if (currentTime - lastEmailTime > 3000) {
                lastEmailTime = currentTime
                val emailPrefix = prefs.getString("email_text", "SÄHKÖPOSTI") ?: "SÄHKÖPOSTI"
                kuulutusTeksti = if (rawTitle.isNotEmpty()) {
                    "$emailPrefix $rawTitle".trim()
                } else {
                    emailPrefix
                }
                logiNimi = if (rawTitle.isNotEmpty()) rawTitle else "Tuntematon"
                sovelluksenNimi = "Sähköposti"
            }
        }

        if (kuulutusTeksti != null) {
            // Lisätään lokiin
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
        // Jos käyttäjä avaa tai poistaa ilmoituksen, toisto lakkaa heti
        if (pkg.contains("whatsapp") || pkg.contains("orca") || pkg.contains("mail") || pkg.contains("gm") || pkg.contains("outlook")) {
            val stopIntent = Intent(this, AlertService::class.java).apply {
                action = "KUITTAA"
            }
            startService(stopIntent)
        }
    }

    private fun onkoYhteystiedoissa(context: Context, nimi: String): Boolean {
        if (nimi.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(nimi))
            val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
            val loytyi = (cursor?.count ?: 0) > 0
            cursor?.close()
            loytyi
        } catch (e: Exception) {
            false
        }
    }

    private fun onkoSahkoposti(pkg: String, notification: Notification): Boolean {
        if (pkg == "com.google.android.gm" ||
            pkg == "com.microsoft.office.outlook" ||
            pkg == "com.samsung.android.email.provider" ||
            pkg == "com.fsck.k9" ||
            pkg == "net.thunderbird.android"
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
