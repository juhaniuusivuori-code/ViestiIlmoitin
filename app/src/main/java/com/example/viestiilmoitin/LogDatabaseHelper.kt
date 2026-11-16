package com.example.viestiilmoitin

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogSummary(
    val sender: String,
    val appType: String,
    val totalCount: Int,
    val datesDetail: String
)

class LogDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "viestiloki.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_LOG = "message_log"
        private const val COL_ID = "id"
        private const val COL_SENDER = "sender"
        private const val COL_APP_TYPE = "app_type"
        private const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_LOG (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SENDER TEXT,
                $COL_APP_TYPE TEXT,
                $COL_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOG")
        onCreate(db)
    }

    fun lisaaMerkinta(sender: String, appType: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_SENDER, sender.trim().ifEmpty { "Tuntematon" })
                put(COL_APP_TYPE, appType)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_LOG, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun haeYhteenveto(): List<LogSummary> {
        val tulokset = mutableListOf<LogSummary>()
        val db = readableDatabase
        val dateFormat = SimpleDateFormat("d.M.yyyy", Locale("fi", "FI"))

        // Haetaan kaikki merkinnät lajiteltuna ajan mukaan
        val cursor = db.rawQuery(
            "SELECT $COL_SENDER, $COL_APP_TYPE, $COL_TIMESTAMP FROM $TABLE_LOG ORDER BY $COL_TIMESTAMP DESC",
            null
        )

        // Ryhmitellään muistissa lähettäjän ja sovelluksen mukaan
        val senderMap = mutableMapOf<Pair<String, String>, MutableList<Long>>()

        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(0)
                val appType = it.getString(1)
                val timestamp = it.getLong(2)
                val key = Pair(sender, appType)
                senderMap.getOrPut(key) { mutableListOf() }.add(timestamp)
            }
        }

        for ((key, timestamps) in senderMap) {
            val (sender, appType) = key
            val totalCount = timestamps.size

            // Ryhmitellään päivät ja lasketaan kappaleet kullekin päivälle
            val dateCounts = mutableMapOf<String, Int>()
            for (ts in timestamps) {
                val pvm = dateFormat.format(Date(ts))
                dateCounts[pvm] = (dateCounts[pvm] ?: 0) + 1
            }

            val paivatTeksti = dateCounts.entries.joinToString(", ") { "${it.key} (${it.value} kpl)" }
            tulokset.add(LogSummary(sender, appType, totalCount, paivatTeksti))
        }

        return tulokset
    }

    fun tyhjennaLoki() {
        try {
            val db = writableDatabase
            db.delete(TABLE_LOG, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
