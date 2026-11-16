package com.example.viestiilmoitin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val prefs = context.getSharedPreferences("Asetukset", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("app_enabled", true)
            if (!isEnabled) {
                return
            }

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val lahettajaNumero = sms.displayOriginatingAddress ?: continue
                val nimi = etsiYhteystiedonNimi(context, lahettajaNumero)

                val prefix = prefs.getString("prefix", "VIESTI TULI ") ?: ""
                val suffix = prefs.getString("suffix", "") ?: ""
                val unknown = prefs.getString("unknown", "VIESTI NUMEROSTA ") ?: ""
                val repeats = prefs.getInt("repeats", 3)
                val intervalMinutes = prefs.getInt("interval", 1) + 1

                val puhuttavaViesti = if (nimi != null) {
                    "$prefix $nimi $suffix".trim()
                } else {
                    "$unknown $lahettajaNumero"
                }

                // Lisätään merkintä viestilokiin
                val tallennettavaNimi = nimi ?: lahettajaNumero
                LogDatabaseHelper(context).lisaaMerkinta(tallennettavaNimi, "Tekstiviesti")

                val serviceIntent = Intent(context, AlertService::class.java).apply {
                    putExtra("MESSAGE", puhuttavaViesti)
                    putExtra("REPEATS_LEFT", repeats)
                    putExtra("INTERVAL_MINUTES", intervalMinutes)
                    putExtra("SENDER", lahettajaNumero)
                    putExtra("IS_SMS", true)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    private fun etsiYhteystiedonNimi(context: Context, numero: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (index != -1) return cursor.getString(index)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
