package com.example.viestiilmoitin

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat

class AppNotificationListener : NotificationListenerService() {

    private var lastWhatsAppTime = 0L
    private var lastMessengerTime = 0L

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

        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            if (currentTime - lastWhatsAppTime > 3000) {
                lastWhatsAppTime = currentTime
                kuulutusTeksti = prefs.getString("whatsapp_text", "WHATSUP VIESTI") ?: "WHATSUP VIESTI"
            }
        } else if (pkg == "com.facebook.orca" || pkg == "com.facebook.mlite") {
            if (currentTime - lastMessengerTime > 3000) {
                lastMessengerTime = currentTime
                kuulutusTeksti = prefs.getString("messenger_text", "MESSENGER VIESTI") ?: "MESSENGER VIESTI"
            }
        }

        if (kuulutusTeksti != null) {
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
        // Jos käyttäjä avaa tai pyyhkii WhatsApp- tai Messenger-ilmoituksen pois, lopetetaan toistohälytys heti
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b" || pkg == "com.facebook.orca" || pkg == "com.facebook.mlite") {
            val stopIntent = Intent(this, AlertService::class.java).apply {
                action = "KUITTAA"
            }
            startService(stopIntent)
        }
    }
}
