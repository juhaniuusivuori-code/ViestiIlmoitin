package com.example.viestiilmoitin

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LokiActivity : AppCompatActivity() {

    private lateinit var tvLogContent: TextView
    private lateinit var btnResetLog: Button
    private lateinit var dbHelper: LogDatabaseHelper

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loki)

        tvLogContent = findViewById(R.id.tvLogContent)
        btnResetLog = findViewById(R.id.btnResetLog)
        dbHelper = LogDatabaseHelper(this)

        paivitaLokiNakyma()

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val resetRunnable = Runnable {
            isLongPressTriggered = true
            dbHelper.tyhjennaLoki()
            paivitaLokiNakyma()
            Toast.makeText(this@LokiActivity, "Loki nollattu onnistuneesti!", Toast.LENGTH_SHORT).show()
        }

        btnResetLog.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressTriggered = false
                    btnResetLog.text = "⏳  Pidetään pohjassa... (1 s)"
                    handler.postDelayed(resetRunnable, 1000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(resetRunnable)
                    btnResetLog.text = "🗑️  NOLLAA LOKI (Pidä pohjassa 1 s)"
                    if (!isLongPressTriggered && event.action == MotionEvent.ACTION_UP) {
                        Toast.makeText(this@LokiActivity, "Pidä nappia pohjassa 1 sekunti nollataksesi", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun paivitaLokiNakyma() {
        val yhteenvedot = dbHelper.haeYhteenveto()
        if (yhteenvedot.isEmpty()) {
            tvLogContent.text = "Ei vielä vastaanotettuja viestejä.\n\nKun viestejä saapuu, niiden lähettäjät, määrät ja päivät tallentuvat tähän automaattisesti."
            return
        }

        val sb = StringBuilder()
        for (item in yhteenvedot) {
            sb.append("📌 ").append(item.sender).append(" (").append(item.appType).append(")\n")
            sb.append("   • Yhteensä: ").append(item.totalCount).append(" viestiä\n")
            sb.append("   • Päivät: ").append(item.datesDetail).append("\n\n")
            sb.append("────────────────────────────\n\n")
        }

        tvLogContent.text = sb.toString().trimEnd()
    }
}
