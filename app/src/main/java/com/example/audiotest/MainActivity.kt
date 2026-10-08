package com.example.audiotest

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private var synthService: AudioSynthesizerService? = null
    private var isBound = false

    private lateinit var switchPower: MaterialSwitch
    private lateinit var tvStatusLabel: TextView
    private lateinit var tvLiveFrequency: TextView
    private lateinit var sineWaveView: SineWaveView

    private lateinit var sliderCenterFreq: Slider
    private lateinit var tvCenterFreqValue: TextView

    private lateinit var sliderModDepth: Slider
    private lateinit var tvModDepthValue: TextView

    private lateinit var sliderModRate: Slider
    private lateinit var tvModRateValue: TextView

    private lateinit var sliderVolume: Slider
    private lateinit var tvVolumeValue: TextView

    private lateinit var chipGroupPresets: ChipGroup

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioSynthesizerService.LocalBinder
            val s = binder.getService()
            synthService = s
            isBound = true

            s.onFrequencyListener = { freq ->
                runOnUiThread {
                    tvLiveFrequency.text = getString(R.string.unit_hz_decimal, freq)
                    sineWaveView.currentFrequency = freq
                }
            }

            syncUiWithService(s)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synthService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkNotificationPermission()
        initViews()
        setupListeners()

        // Bind to AudioSynthesizerService
        val intent = Intent(this, AudioSynthesizerService::class.java)
        startService(intent) // Ensure service lifecycle
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initViews() {
        switchPower = findViewById(R.id.switchPower)
        tvStatusLabel = findViewById(R.id.tvStatusLabel)
        tvLiveFrequency = findViewById(R.id.tvLiveFrequency)
        sineWaveView = findViewById(R.id.sineWaveView)

        sliderCenterFreq = findViewById(R.id.sliderCenterFreq)
        tvCenterFreqValue = findViewById(R.id.tvCenterFreqValue)

        sliderModDepth = findViewById(R.id.sliderModDepth)
        tvModDepthValue = findViewById(R.id.tvModDepthValue)

        sliderModRate = findViewById(R.id.sliderModRate)
        tvModRateValue = findViewById(R.id.tvModRateValue)

        sliderVolume = findViewById(R.id.sliderVolume)
        tvVolumeValue = findViewById(R.id.tvVolumeValue)

        chipGroupPresets = findViewById(R.id.chipGroupPresets)
    }

    private fun setupListeners() {
        switchPower.setOnCheckedChangeListener { _, isChecked ->
            synthService?.let { service ->
                if (isChecked) {
                    val intent = Intent(this, AudioSynthesizerService::class.java).apply {
                        action = AudioSynthesizerService.ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    service.startSynthesizer()
                } else {
                    service.stopSynthesizer()
                }
                updatePowerStatusUi(isChecked)
            }
        }

        sliderCenterFreq.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                synthService?.centerFrequency = value
                tvCenterFreqValue.text = getString(R.string.unit_hz, value.toInt())
            }
        }

        sliderModDepth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                synthService?.modulationDepth = value
                tvModDepthValue.text = getString(R.string.unit_hz, value.toInt())
            }
        }

        sliderModRate.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                synthService?.modulationRate = value
                tvModRateValue.text = getString(R.string.unit_hz_rate, value)
            }
        }

        sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                synthService?.volume = value
                tvVolumeValue.text = getString(R.string.unit_percent, (value * 100).toInt())
                sineWaveView.volume = value
            }
        }

        chipGroupPresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipPresetSiren -> applyPreset(440f, 150f, 1.0f)
                    R.id.chipPresetWobble -> applyPreset(600f, 250f, 3.5f)
                    R.id.chipPresetVibrato -> applyPreset(440f, 25f, 4.5f)
                    R.id.chipPresetSweep -> applyPreset(300f, 200f, 0.3f)
                    R.id.chipPresetLaser -> applyPreset(800f, 400f, 2.5f)
                }
            }
        }
    }

    private fun applyPreset(center: Float, depth: Float, rate: Float) {
        sliderCenterFreq.value = center
        sliderModDepth.value = depth
        sliderModRate.value = rate

        tvCenterFreqValue.text = getString(R.string.unit_hz, center.toInt())
        tvModDepthValue.text = getString(R.string.unit_hz, depth.toInt())
        tvModRateValue.text = getString(R.string.unit_hz_rate, rate)

        synthService?.let { service ->
            service.centerFrequency = center
            service.modulationDepth = depth
            service.modulationRate = rate
        }
    }

    private fun syncUiWithService(service: AudioSynthesizerService) {
        val active = service.isSynthesizing
        switchPower.isChecked = active
        updatePowerStatusUi(active)

        val center = service.centerFrequency
        val depth = service.modulationDepth
        val rate = service.modulationRate
        val vol = service.volume

        sliderCenterFreq.value = center
        sliderModDepth.value = depth
        sliderModRate.value = rate
        sliderVolume.value = vol

        tvCenterFreqValue.text = getString(R.string.unit_hz, center.toInt())
        tvModDepthValue.text = getString(R.string.unit_hz, depth.toInt())
        tvModRateValue.text = getString(R.string.unit_hz_rate, rate)
        tvVolumeValue.text = getString(R.string.unit_percent, (vol * 100).toInt())

        sineWaveView.volume = vol
    }

    private fun updatePowerStatusUi(active: Boolean) {
        if (active) {
            tvStatusLabel.text = getString(R.string.status_on)
            tvStatusLabel.setTextColor(Color.parseColor("#00E676")) // Neon green
            sineWaveView.isSynthesizing = true
        } else {
            tvStatusLabel.text = getString(R.string.status_off)
            tvStatusLabel.setTextColor(Color.parseColor("#FF5252")) // Red
            tvLiveFrequency.text = getString(R.string.unit_hz, 0)
            sineWaveView.isSynthesizing = false
        }
    }

    override fun onDestroy() {
        if (isBound) {
            synthService?.onFrequencyListener = null
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
