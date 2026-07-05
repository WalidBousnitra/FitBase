package com.fitbase.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * Feedback sensorial centralizado: Haptic + Audio funcional.
 *
 * Haptic: VibrationEffect con patrones y amplitudes calibradas.
 * Audio: SoundPool con tonos generados programáticamente (sin assets externos).
 *        Usa el stream USAGE_ASSISTANCE_SONIFICATION para respetar el modo silencio.
 *
 * Patrones (Design System):
 *   - TAP:     Pulsación ligera al presionar botones
 *   - CONFIRM: Confirmación de acción exitosa (serie registrada)
 *   - SUCCESS: Logro mayor (sesión completada)
 *   - ERROR:   Acción bloqueada o fallo
 *   - TICK:    Pulso rítmico en countdown
 */
public final class FeedbackHelper {

    // Singleton
    private static FeedbackHelper instance;

    private final Vibrator vibrator;
    private final AudioManager audioManager;
    private final java.io.File cacheDir;
    private SoundPool soundPool;
    private int soundTick;
    private int soundConfirm;
    private int soundSuccess;
    private int soundError;
    private boolean soundsLoaded = false;

    // Amplitudes (0-255): calibradas para que sean sutiles, no intrusivas
    private static final int AMP_LIGHT = 40;
    private static final int AMP_MEDIUM = 100;
    private static final int AMP_STRONG = 180;

    private FeedbackHelper(Context context) {
        Context appContext = context.getApplicationContext();
        this.cacheDir = appContext.getCacheDir();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

        // Obtener Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
        }

        // SoundPool: máx 4 streams simultáneos, USAGE_ASSISTANCE_SONIFICATION
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) soundsLoaded = true;
        });

        generarSonidos(appContext);
    }

    public static synchronized FeedbackHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FeedbackHelper(context);
        }
        return instance;
    }

    // ─── HAPTIC API ────────────────────────────────────────────────────────

    /** Pulsación ligera — botones, navegación. 10ms, amplitud baja. */
    public void tap() {
        vibrate(10, AMP_LIGHT);
    }

    public void vibrateLight() {
        vibrate(10, AMP_LIGHT);
    }

    public void vibrateStrong() {
        vibrate(50, AMP_STRONG);
    }

    /** Confirmación — serie registrada, acción completada. 25ms, amplitud media. */
    public void confirm() {
        vibrate(25, AMP_MEDIUM);
        playSound(soundConfirm, 0.3f);
    }

    /** Éxito mayor — sesión completada, logro. Doble pulso 20+30ms. */
    public void success() {
        long[] pattern = {0, 20, 60, 30};
        int[] amplitudes = {0, AMP_MEDIUM, 0, AMP_STRONG};
        vibratePattern(pattern, amplitudes);
        playSound(soundSuccess, 0.4f);
    }

    /** Error/Bloqueo — acción denegada. Doble pulso corto e intenso. */
    public void error() {
        long[] pattern = {0, 30, 50, 30};
        int[] amplitudes = {0, AMP_STRONG, 0, AMP_STRONG};
        vibratePattern(pattern, amplitudes);
        playSound(soundError, 0.25f);
    }

    /** Tick rítmico — timer countdown, cada segundo en últimos 5. 8ms ultra-ligero. */
    public void tick() {
        vibrate(8, AMP_LIGHT);
        playSound(soundTick, 0.15f);
    }

    // ─── AUDIO API ─────────────────────────────────────────────────────────

    /**
     * Genera tonos cortos en PCM 16-bit y los carga en SoundPool.
     * Sonidos secos, orgánicos: sinusoidales muy cortos con envolvente rápida.
     */
    private void generarSonidos(Context context) {
        // Tick: 880Hz, 30ms — chasquido seco
        soundTick = cargarTono(880, 30, 0.6f);
        // Confirm: 660Hz, 50ms — nota media, satisfactoria
        soundConfirm = cargarTono(660, 50, 0.8f);
        // Success: 880Hz→1320Hz chirp, 80ms — tono ascendente corto
        soundSuccess = cargarTonoChirp(880, 1320, 80, 0.9f);
        // Error: 220Hz, 60ms — tono grave, seco
        soundError = cargarTono(220, 60, 0.7f);
    }

    /**
     * Genera un tono sinusoidal puro con fade-in/fade-out exponencial.
     * @param freqHz Frecuencia en Hz
     * @param durationMs Duración en ms
     * @param amplitude 0.0 - 1.0
     * @return SoundPool ID
     */
    private int cargarTono(int freqHz, int durationMs, float amplitude) {
        int sampleRate = 44100;
        int numSamples = (sampleRate * durationMs) / 1000;
        short[] samples = new short[numSamples];

        double twoPiF = 2.0 * Math.PI * freqHz;
        int fadeLen = Math.min(numSamples / 4, sampleRate / 100); // fade: 10ms o 25% del total

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double sine = Math.sin(twoPiF * t);

            // Envolvente: fade-in y fade-out exponencial para evitar clicks
            double env = 1.0;
            if (i < fadeLen) {
                env = (double) i / fadeLen;
                env = env * env; // cuadrática = más orgánico
            } else if (i > numSamples - fadeLen) {
                env = (double) (numSamples - i) / fadeLen;
                env = env * env;
            }

            samples[i] = (short) (sine * env * amplitude * Short.MAX_VALUE);
        }

        return cargarPCMEnPool(samples, sampleRate);
    }

    /**
     * Genera un chirp (tono con frecuencia ascendente) para el efecto de éxito.
     */
    private int cargarTonoChirp(int freqStart, int freqEnd, int durationMs, float amplitude) {
        int sampleRate = 44100;
        int numSamples = (sampleRate * durationMs) / 1000;
        short[] samples = new short[numSamples];

        int fadeLen = Math.min(numSamples / 4, sampleRate / 100);

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double progress = (double) i / numSamples;
            double freq = freqStart + (freqEnd - freqStart) * progress;
            double sine = Math.sin(2.0 * Math.PI * freq * t);

            double env = 1.0;
            if (i < fadeLen) {
                env = (double) i / fadeLen;
                env = env * env;
            } else if (i > numSamples - fadeLen) {
                env = (double) (numSamples - i) / fadeLen;
                env = env * env;
            }

            samples[i] = (short) (sine * env * amplitude * Short.MAX_VALUE);
        }

        return cargarPCMEnPool(samples, sampleRate);
    }

    /**
     * Empaqueta samples PCM 16-bit mono en un WAV en memoria y lo carga en SoundPool.
     */
    private int cargarPCMEnPool(short[] samples, int sampleRate) {
        // WAV header (44 bytes) + PCM data
        int dataSize = samples.length * 2;
        int fileSize = 44 + dataSize;
        byte[] wav = new byte[fileSize];

        // RIFF header
        wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
        writeInt(wav, 4, fileSize - 8);
        wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';

        // fmt chunk
        wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
        writeInt(wav, 16, 16);          // chunk size
        writeShort(wav, 20, (short) 1); // PCM
        writeShort(wav, 22, (short) 1); // mono
        writeInt(wav, 24, sampleRate);
        writeInt(wav, 28, sampleRate * 2); // byte rate
        writeShort(wav, 32, (short) 2);    // block align
        writeShort(wav, 34, (short) 16);   // bits per sample

        // data chunk
        wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
        writeInt(wav, 40, dataSize);

        // PCM samples
        for (int i = 0; i < samples.length; i++) {
            wav[44 + i * 2] = (byte) (samples[i] & 0xFF);
            wav[44 + i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }

        return cargarWavDesdeMemoria(wav);
    }

    /**
     * Carga un WAV desde byte[] en SoundPool usando un archivo temporal en caché interna.
     */
    private int cargarWavDesdeMemoria(byte[] wavData) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("fb_snd_", ".wav", cacheDir);
            tempFile.deleteOnExit();

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(wavData);
                fos.flush();
            }

            int soundId = soundPool.load(tempFile.getAbsolutePath(), 1);
            return soundId;
        } catch (Exception e) {
            return 0;
        }
    }

    // ─── VIBRATION INTERNALS ───────────────────────────────────────────────

    private void vibrate(int durationMs, int amplitude) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        VibrationEffect effect = VibrationEffect.createOneShot(durationMs, amplitude);
        vibrator.vibrate(effect);
    }

    private void vibratePattern(long[] pattern, int[] amplitudes) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        VibrationEffect effect = VibrationEffect.createWaveform(pattern, amplitudes, -1);
        vibrator.vibrate(effect);
    }

    private void playSound(int soundId, float volume) {
        if (soundId > 0 && soundsLoaded && soundPool != null && puedeReproducirAudio()) {
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f);
        }
    }

    /**
     * Audio solo en modo normal O si hay auriculares conectados.
     * App 100% usable sin sonido: animaciones + haptic dan feedback suficiente.
     */
    private boolean puedeReproducirAudio() {
        if (audioManager == null) return false;
        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) return true;
        return hayAuricularesConectados();
    }

    private boolean hayAuricularesConectados() {
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                return true;
            }
        }
        return false;
    }

    // ─── WAV BYTE UTILS ────────────────────────────────────────────────────

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /** Liberar recursos al cerrar la app. Llamar desde Application.onTerminate(). */
    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
