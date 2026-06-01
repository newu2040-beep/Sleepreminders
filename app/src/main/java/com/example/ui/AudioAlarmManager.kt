package com.example.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import kotlin.math.sin

class AudioAlarmManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechText: String? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var isPlayingSynth = false
    private var synthThread: Thread? = null

    // Preview support
    private var previewPlayer: MediaPlayer? = null
    private var previewHandler: android.os.Handler? = null
    private var previewRunnable: Runnable? = null

    // Alarm trimming surveillance
    private var alarmCheckHandler: android.os.Handler? = null
    private var alarmCheckRunnable: Runnable? = null

    // Ambient sound support
    private var ambientCountdownHandler: android.os.Handler? = null
    private var ambientCountdownRunnable: Runnable? = null

    init {
        activeManager = this
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("AudioAlarmManager", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                val result = it.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                    pendingSpeechText?.let { text ->
                        speakText(text)
                        pendingSpeechText = null
                    }
                }
            }
        }
    }

    fun playPreview(uriString: String, startMs: Long, endMs: Long) {
        stopPreview()
        try {
            previewPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriString))
                prepare()
                seekTo(startMs.toInt())
                start()
            }

            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            previewHandler = handler
            val runnable = object : Runnable {
                override fun run() {
                    previewPlayer?.let { player ->
                        try {
                            if (player.isPlaying) {
                                if (player.currentPosition >= endMs.toInt() || player.currentPosition < startMs.toInt() - 250) {
                                    player.seekTo(startMs.toInt())
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AudioAlarmManager", "Preview trim loop check failed", e)
                        }
                        previewHandler?.postDelayed(this, 100)
                    }
                }
            }
            previewRunnable = runnable
            handler.post(runnable)
        } catch (e: Exception) {
            Log.e("AudioAlarmManager", "Failed to play custom preview", e)
        }
    }

    fun stopPreview() {
        try {
            previewHandler?.let { handler ->
                previewRunnable?.let { runnable ->
                    handler.removeCallbacks(runnable)
                }
            }
            previewHandler = null
            previewRunnable = null
        } catch (e: Exception) {}

        try {
            previewPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            previewPlayer = null
        } catch (e: Exception) {}
    }

    fun playAlert(
        soundId: String, 
        customUriString: String? = null, 
        voiceText: String? = null,
        customStartMs: Long = 0L,
        customEndMs: Long = -1L
    ) {
        stopAll()

        if (soundId == "custom_audio" && customUriString != null) {
            // Play custom audio selected from device storage
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(customUriString))
                    prepare()
                    seekTo(customStartMs.toInt())
                    start()
                }

                if (customEndMs > customStartMs) {
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    alarmCheckHandler = handler
                    val runnable = object : Runnable {
                        override fun run() {
                            mediaPlayer?.let { player ->
                                try {
                                    if (player.isPlaying) {
                                        if (player.currentPosition >= customEndMs.toInt() || player.currentPosition < customStartMs.toInt() - 250) {
                                            player.seekTo(customStartMs.toInt())
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AudioAlarmManager", "Alarm loop check failed", e)
                                }
                                alarmCheckHandler?.postDelayed(this, 100)
                            }
                        }
                    }
                    alarmCheckRunnable = runnable
                    handler.post(runnable)
                } else {
                    mediaPlayer?.isLooping = true
                }
                return
            } catch (e: Exception) {
                Log.e("AudioAlarmManager", "Failed to play custom URI: $customUriString, falling back", e)
            }
        }

        // Handle voice reminders & standard voice templates via TTS
        if (soundId.startsWith("voice_") || voiceText != null) {
            val sentence = voiceText ?: when (soundId) {
                "voice_gentle" -> "Good morning. Your sleep session has completed. Please wake up gently."
                "voice_narrator" -> "You have completed your scheduled sleep session successfully. Breathe in, and embrace the day."
                "voice_female" -> "Good morning. Your sleep session has completed."
                "voice_male" -> "Time to wake up and start your day."
                "voice_motivational" -> "Rise and shine! You planned your sleep, completed it, and now you are ready to conquer the day!"
                else -> "Good morning. Your sleep session has completed."
            }

            // Adjust TTS voice parameters depending on theme
            tts?.setPitch(if (soundId == "voice_gentle") 1.2f else if (soundId == "voice_male") 0.85f else 1.0f)
            tts?.setSpeechRate(if (soundId == "voice_narrator") 0.85f else 1.0f)

            if (isTtsInitialized) {
                speakText(sentence)
            } else {
                pendingSpeechText = sentence
            }
            return
        }

        // Synthesize ambient sounds using AudioTrack
        startSyntheticAudio(soundId)
    }

    private fun speakText(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WakeUpSpeechID")
            // After speaking, loop a soft synthetic tone so it doesn't just stop
            Thread {
                Thread.sleep(6000)
                if (!isPlayingSynth && mediaPlayer == null) {
                    startSyntheticAudio("sound_soft_alarm")
                }
            }.start()
        } catch (e: Exception) {
            Log.e("AudioAlarmManager", "Error speaking TTS", e)
        }
    }

    private fun startSyntheticAudio(type: String) {
        isPlayingSynth = true
        synthThread = Thread {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()
            } catch (e: Exception) {
                Log.e("AudioAlarmManager", "Failed to initialize AudioTrack", e)
                return@Thread
            }

            val bufferSize = 10000
            val samples = ShortArray(bufferSize)
            var angle = 0.0

            while (isPlayingSynth) {
                when (type) {
                    "sound_white_noise" -> {
                        // Synthesize pure steady white noise
                        for (i in samples.indices) {
                            val noise = (Math.random() * 65536 - 32768)
                            samples[i] = (noise * 0.08).toInt().toShort()
                        }
                    }
                    "sound_rain" -> {
                        // Synthesize rain using filtered random noise (brown/white noise approximation)
                        for (i in samples.indices) {
                            var value = (Math.random() * 65536 - 32768).toInt()
                            // Run a basic low-pass filter to make it sound like gentle rain/rumble
                            value = (value * 0.15).toInt()
                            samples[i] = value.toShort()
                        }
                    }
                    "sound_ocean" -> {
                        // Synthesize slow rolling sea waves: slow-frequency amplitude-modulated noise
                        val t = System.currentTimeMillis() / 1000.0
                        val waveAmplitude = (sin(t * 0.25) + 1.0) / 2.0 // slow cycle, 25 seconds
                        for (i in samples.indices) {
                            val noise = (Math.random() * 65536 - 32768)
                            // low-pass soft rumble
                            val sampleValue = (noise * 0.12 * waveAmplitude).toInt()
                            samples[i] = sampleValue.toShort()
                        }
                    }
                    "sound_birds" -> {
                        // Synthesize intermittent pleasant chirps using high frequency FM sweep
                        val timeMs = System.currentTimeMillis()
                        val isChirping = (timeMs % 4000) < 1200 // Chirp for 1.2s every 4s
                        val chirpCycle = (timeMs % 1200) / 1200.0
                        for (i in samples.indices) {
                            if (isChirping) {
                                val chirpFreq = 2200.0 + 1500.0 * sin(chirpCycle * Math.PI)
                                val sampleAngle = 2.0 * Math.PI * chirpFreq / sampleRate
                                angle += sampleAngle
                                samples[i] = (sin(angle) * 12000).toInt().toShort()
                            } else {
                                samples[i] = 0
                            }
                        }
                    }
                    "sound_wind" -> {
                        // Synthesize wind: low-pass white noise with rising/falling wind swell
                        val t = System.currentTimeMillis() / 1000.0
                        val windSwell = 0.4 + 0.6 * ((sin(t * 0.15) + sin(t * 0.37)) / 2.0 + 1.0) / 2.0
                        for (i in samples.indices) {
                            val noise = (Math.random() * 65536 - 32768)
                            val value = (noise * 0.08 * windSwell).toInt()
                            samples[i] = value.toShort()
                        }
                    }
                    "sound_thunder" -> {
                        // Synthesize thunderstorm: rain background with periodic lightning/thunder rolls
                        val t = System.currentTimeMillis()
                        val isThunder = (t % 12000) < 3200 // thunder rolls for 3.2s every 12s
                        val thunderAge = (t % 12000) / 3200.0
                        // thunder intensity declines back slowly
                        val thunderVolume = if (isThunder) {
                            sin(thunderAge * Math.PI) * (1.0 - thunderAge)
                        } else 0.0
                        for (i in samples.indices) {
                            val rainNoise = (Math.random() * 65536 - 32768) * 0.08
                            val thunderNoise = if (isThunder) {
                                (Math.random() * 65536 - 32768) * 0.25 * thunderVolume * (0.8 + 0.2 * sin(i * 0.005))
                            } else 0.0
                            val value = (rainNoise + thunderNoise).toInt().coerceIn(-32768, 32767)
                            samples[i] = value.toShort()
                        }
                    }
                    "sound_stream" -> {
                        // Synthesize babbling brook: water rushing with random rapid water ripples
                        val t = System.currentTimeMillis() / 1000.0
                        for (i in samples.indices) {
                            val noise = (Math.random() * 65536 - 32768)
                            val ripple = 0.5 + 0.5 * sin(t * 8.0 + sin(i * 0.002) * 2.0)
                            val value = (noise * 0.06 * ripple).toInt()
                            samples[i] = value.toShort()
                        }
                    }
                    "sound_frogs" -> {
                        // Synthesize tree frogs: periodic rhythmic high/low chirps
                        val t = System.currentTimeMillis()
                        val playFrogs = (t % 3000) < 1400
                        val frogCycle = (t % 1400) / 1400.0
                        for (i in samples.indices) {
                            if (playFrogs) {
                                val rate = 650.0 + 80.0 * sin(frogCycle * 25.0 * Math.PI)
                                val sampleAngle = 2.0 * Math.PI * rate / sampleRate
                                angle += sampleAngle
                                samples[i] = (sin(angle) * 7000).toInt().toShort()
                            } else {
                                samples[i] = 0
                            }
                        }
                    }
                    "sound_campfire" -> {
                        // Synthesize crackling campfire: wood rumble + sudden spark crackles/clicks
                        for (i in samples.indices) {
                            val woodRumble = (Math.random() * 65536 - 32768) * 0.04
                            val isSparkClick = Math.random() > 0.9995
                            val sparkNoise = if (isSparkClick) {
                                (Math.random() * 65536 - 32768) * 0.6
                            } else 0.0
                            val value = (woodRumble + sparkNoise).toInt().coerceIn(-32768, 32767)
                            samples[i] = value.toShort()
                        }
                    }
                    "sound_custom_mix" -> {
                        // Custom Mix of Wind, Rain, Stream, Campfire, and gentle Chimes
                        val tSec = System.currentTimeMillis() / 1000.0
                        val tMs = System.currentTimeMillis()

                        val windF = customWindVol.value / 100f
                        val rainF = customRainVol.value / 100f
                        val streamF = customStreamVol.value / 100f
                        val campfireF = customCampfireVol.value / 100f
                        val chimesF = customChimesVol.value / 100f

                        val windSwell = 0.4 + 0.6 * ((sin(tSec * 0.15) + sin(tSec * 0.37)) / 2.0 + 1.0) / 2.0
                        val streamRipple = 0.5 + 0.5 * sin(tSec * 8.0)

                        val chimeSec = (tMs / 1000) % 8
                        val baseFreq = when (chimeSec) {
                            0L, 1L -> 440.0 // A4
                            2L -> 523.25    // C5
                            3L, 4L -> 587.33 // D5
                            5L -> 659.25   // E5
                            else -> 783.99  // G5
                        }
                        val volEnvelope = sin((tMs % 1000) / 1000.0 * Math.PI)

                        for (i in samples.indices) {
                            val noise = Math.random() * 65536 - 32768

                            // Wind
                            val windVal = noise * 0.08 * windSwell * windF

                            // Rain
                            val rainVal = noise * 0.12 * rainF

                            // Stream
                            val streamVal = noise * 0.07 * streamRipple * streamF

                            // Campfire
                            val campfireVal = (noise * 0.04 + (if (Math.random() > 0.9996) noise * 0.5 else 0.0)) * campfireF

                            // Chimes
                            val chimesSampleAngle = 2.0 * Math.PI * baseFreq / sampleRate
                            angle += chimesSampleAngle
                            val chimeVal = (sin(angle) + 0.4 * sin(2.0 * angle)) * 6000.0 * volEnvelope * chimesF

                            val blended = (windVal + rainVal + streamVal + campfireVal + chimeVal).toInt().coerceIn(-32768, 32767)
                            samples[i] = blended.toShort()
                        }
                    }
                    else -> { // "sound_soft_alarm" or alarm tones
                        // Synthesize a calming, pentatonic chime progression (A4, C5, D5, E5, G5)
                        val chimeSec = (System.currentTimeMillis() / 1000) % 6
                        val baseFreq = when (chimeSec) {
                            0L, 1L -> 440.0 // A4
                            2L -> 523.25    // C5
                            3L, 4L -> 587.33 // D5
                            else -> 659.25   // E5
                        }
                        val volEnvelope = sin((System.currentTimeMillis() % 1000) / 1000.0 * Math.PI)
                        for (i in samples.indices) {
                            val sampleAngle = 2.0 * Math.PI * baseFreq / sampleRate
                            angle += sampleAngle
                            // Dual harmonics for a richer, bell-like timbre
                            var v = sin(angle) + 0.5 * sin(2.0 * angle) + 0.25 * sin(3.0 * angle)
                            v *= 8000.0 * volEnvelope
                            samples[i] = v.toInt().toShort()
                        }
                    }
                }
                audioTrack?.write(samples, 0, samples.size)
            }
        }
        synthThread?.start()
    }

    fun stopAll() {
        isPlayingSynth = false
        stopPreview()

        try {
            alarmCheckHandler?.let { handler ->
                alarmCheckRunnable?.let { runnable ->
                    handler.removeCallbacks(runnable)
                }
            }
            alarmCheckHandler = null
            alarmCheckRunnable = null
        } catch (e: Exception) {}

        try {
            ambientCountdownHandler?.let { handler ->
                ambientCountdownRunnable?.let { runnable ->
                    handler.removeCallbacks(runnable)
                }
            }
            ambientCountdownHandler = null
            ambientCountdownRunnable = null
        } catch (e: Exception) {}

        try {
            synthThread?.interrupt()
            synthThread = null
        } catch (e: Exception) {}

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {}

        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
            audioTrack = null
        } catch (e: Exception) {}

        try {
            tts?.stop()
        } catch (e: Exception) {}

        playingAmbientId.value = null
        ambientTimeRemainingSec.value = 0
    }

    fun playAmbient(soundId: String, durationMinutes: Int) {
        stopAll()

        startSyntheticAudio(soundId)

        playingAmbientId.value = soundId
        val totalSecs = if (durationMinutes > 0) durationMinutes * 60 else -1
        ambientTimeRemainingSec.value = totalSecs

        val soundName = getSoundName(soundId)
        val durationText = if (durationMinutes > 0) "Duration: $durationMinutes min" else "Duration: Infinite"
        showAmbientNotification(soundName, durationText)

        if (totalSecs > 0) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            ambientCountdownHandler = handler
            val runnable = object : Runnable {
                var secsLeft = totalSecs
                override fun run() {
                    secsLeft--
                    ambientTimeRemainingSec.value = secsLeft
                    if (secsLeft <= 0) {
                        stopActiveAmbient()
                    } else {
                        if (secsLeft % 10 == 0) {
                            val mins = secsLeft / 60
                            val secs = secsLeft % 60
                            val remText = String.format("Remaining time: %02d:%02d", mins, secs)
                            showAmbientNotification(soundName, remText)
                        }
                        ambientCountdownHandler?.postDelayed(this, 1000)
                    }
                }
            }
            ambientCountdownRunnable = runnable
            handler.postDelayed(runnable, 1000)
        }
    }

    fun showAmbientNotification(soundName: String, remainingTimeText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_AMBIENT,
                "Ambient Nature Sounds",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active playback controls for ambient nature sounds."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            12346,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop intent
        val stopIntent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_STOP_AMBIENT
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            12347,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_AMBIENT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Playing Nature Sound: $soundName")
            .setContentText("Enjoy relaxing ambient sounds. $remainingTimeText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_media_pause, "Stop Sound", stopPendingIntent)

        notificationManager.notify(NOTIFICATION_ID_AMBIENT, builder.build())
    }

    private fun getSoundName(soundId: String): String {
        return when (soundId) {
            "sound_rain" -> "Rain Storm 🌧️"
            "sound_ocean" -> "Ocean Waves 🌊"
            "sound_birds" -> "Forest Birds 🐦"
            "sound_white_noise" -> "Steady White Noise 💨"
            "sound_soft_alarm" -> "Calming Chimes 🔔"
            "sound_wind" -> "Gentle Breezes 🍃"
            "sound_thunder" -> "Cosmic Thunderstorm ⛈️"
            "sound_stream" -> "Babbling Brook 🌊"
            "sound_frogs" -> "Summer Frogs 🐸"
            "sound_campfire" -> "Crackling Campfire 🔥"
            "sound_custom_mix" -> "Custom Sound Mix 🎛️"
            else -> "Ambient Sound"
        }
    }

    fun release() {
        stopAll()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {}
    }

    companion object {
        var activeManager: AudioAlarmManager? = null

        const val NOTIFICATION_ID_AMBIENT = 12345
        const val NOTIFICATION_CHANNEL_AMBIENT = "ambient_sounds_channel"

        val playingAmbientId = MutableStateFlow<String?>(null)
        val ambientTimeRemainingSec = MutableStateFlow(0)

        // Custom mix intensities (0f to 100f)
        val customWindVol = MutableStateFlow(30f)
        val customRainVol = MutableStateFlow(20f)
        val customStreamVol = MutableStateFlow(10f)
        val customCampfireVol = MutableStateFlow(40f)
        val customChimesVol = MutableStateFlow(15f)

        fun stopActiveAmbient() {
            playingAmbientId.value = null
            ambientTimeRemainingSec.value = 0
            activeManager?.let { manager ->
                manager.stopAll()
                try {
                    val nm = manager.context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.cancel(NOTIFICATION_ID_AMBIENT)
                } catch (e: Exception) {}
            }
        }
    }
}
