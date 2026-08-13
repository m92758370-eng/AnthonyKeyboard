package com.anthonykeyboard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var speedLabel: TextView
    private lateinit var speedSeekBar: SeekBar
    private lateinit var autoTypeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (RemoteStatusHelper.blockIfDisabled(this)) return

        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btnSwitchKeyboard).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        autoTypeEditText = findViewById(R.id.autoTypeEditText)
        autoTypeEditText.setText(PrefsHelper.getAutoTypeText(this))

        speedLabel = findViewById(R.id.speedLabel)
        speedSeekBar = findViewById(R.id.speedSeekBar)
        val currentDelay = PrefsHelper.getAutoTypeDelayMs(this)
        speedSeekBar.progress = (currentDelay - PrefsHelper.MIN_DELAY_MS).toInt()
        updateSpeedLabel(currentDelay)
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSpeedLabel(PrefsHelper.MIN_DELAY_MS + progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnSaveAutoType).setOnClickListener {
            PrefsHelper.setAutoTypeText(this, autoTypeEditText.text.toString())
            val delay = PrefsHelper.MIN_DELAY_MS + speedSeekBar.progress
            PrefsHelper.setAutoTypeDelayMs(this, delay)
            Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSpeedLabel(delayMs: Long) {
        val charsPerSec = 1000.0 / delayMs
        speedLabel.text = "سرعت تایپ: هر حرف %dms (%.1f حرف در ثانیه)".format(delayMs, charsPerSec)
    }

    override fun onPause() {
        super.onPause()
        if (::autoTypeEditText.isInitialized) {
            PrefsHelper.setAutoTypeText(this, autoTypeEditText.text.toString())
        }
    }
}
