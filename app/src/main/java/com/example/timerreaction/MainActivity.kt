package com.example.timerreaction

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timer: TextView
    private lateinit var button: Button
    private lateinit var offsetText: TextView
    private var running = false
    private var startNanos = 0L
    private var vibrated = false
    private var leadMillis = 20L
    private val targetNanos = 10_000_000_000L
    private val vibrator by lazy {
        if (android.os.Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
    }
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val elapsed = (SystemClock.elapsedRealtimeNanos() - startNanos).coerceAtLeast(0L)
            val millis = (elapsed / 1_000_000L).coerceAtMost(10_000L)
            timer.text = String.format(Locale.US, "%02d:%02d.%02d", 0, millis / 1000, (millis % 1000) / 10)
            if (!vibrated && elapsed >= targetNanos - leadMillis * 1_000_000L) {
                vibrated = true
                if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            if (elapsed >= targetNanos) {
                running = false
                button.text = "СБРОС"
            } else handler.postDelayed(this, 1L)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(32, 32, 32, 32); setBackgroundColor(Color.rgb(17,24,39))
        }
        val heading = TextView(this).apply { text = "ТАЙМЕР РЕАКЦИИ"; textSize = 20f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        timer = TextView(this).apply { text = "00:00.00"; textSize = 54f; typeface = android.graphics.Typeface.MONOSPACE; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        button = Button(this).apply { text = "ПУСК"; textSize = 23f; setOnClickListener { if (running || this.text == "СБРОС") reset() else start() } }
        offsetText = TextView(this).apply { textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
        val slider = SeekBar(this).apply { max = 500; progress = 20; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { leadMillis = progress.toLong(); offsetText.text = "Вибрация за $progress мс до 10 секунд" }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }) }
        offsetText.text = "Вибрация за 20 мс до 10 секунд"
        val note = TextView(this).apply { text = "Сигнал не гарантирует точность нажатия: задержки Android и реакция человека различаются."; setTextColor(Color.GRAY); textSize = 13f; gravity = Gravity.CENTER }
        listOf(heading, timer, button, offsetText, slider, note).forEach { view -> root.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 24 }) }
        setContentView(root)
    }
    private fun start() {
        handler.removeCallbacks(tick); vibrated = false; running = true; startNanos = SystemClock.elapsedRealtimeNanos()
        button.text = "СТОП / СБРОС"; tick.run()
    }
    private fun reset() {
        running = false; handler.removeCallbacks(tick); vibrator.cancel(); timer.text = "00:00.00"; button.text = "ПУСК"
    }
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
}
