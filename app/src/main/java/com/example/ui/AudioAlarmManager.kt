package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
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

    init {
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
    }

    fun release() {
        stopAll()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {}
    }
}
