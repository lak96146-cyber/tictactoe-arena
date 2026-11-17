package audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Procedural Audio Synthesizer for Tic-Tac-Toe Arena.
 * Generates custom 16-bit PCM sound effects dynamically using pure standard Java.
 * Requires zero external audio files and runs asynchronously without blocking the UI.
 */
public class SoundManager {
    private static final SoundManager INSTANCE = new SoundManager();

    private final ExecutorService soundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Audio-Synthesizer");
        t.setDaemon(true);
        return t;
    });

    private boolean muted = false;

    private SoundManager() {}

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isMuted() {
        return muted;
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
    }

    public synchronized void toggleMute() {
        this.muted = !this.muted;
    }

    /**
     * Plays move placement sound (short futuristic laser/chime).
     */
    public void playMove() {
        if (muted) return;
        soundExecutor.submit(() -> {
            byte[] data = synthesizeToneSweep(520, 880, 0.08f, 0.6f);
            playPcm(data, 44100);
        });
    }

    /**
     * Plays victory fanfare (triumphant 4-note arpeggio: C5, E5, G5, C6).
     */
    public void playWin() {
        if (muted) return;
        soundExecutor.submit(() -> {
            int[] notes = {523, 659, 784, 1046}; // C5, E5, G5, C6
            byte[] fanfare = synthesizeArpeggio(notes, 0.12f, 0.4f, 0.7f);
            playPcm(fanfare, 44100);
        });
    }

    /**
     * Plays draw match sound (mellow dual-tone chord).
     */
    public void playDraw() {
        if (muted) return;
        soundExecutor.submit(() -> {
            byte[] chord = synthesizeDualTone(440, 330, 0.35f, 0.5f);
            playPcm(chord, 44100);
        });
    }

    /**
     * Plays subtle UI click sound.
     */
    public void playClick() {
        if (muted) return;
        soundExecutor.submit(() -> {
            byte[] click = synthesizeToneSweep(1200, 600, 0.03f, 0.35f);
            playPcm(click, 44100);
        });
    }

    /**
     * Plays error or invalid move sound (low subtle warning buzz).
     */
    public void playError() {
        if (muted) return;
        soundExecutor.submit(() -> {
            byte[] err = synthesizeTone(160, 0.15f, 0.5f, true);
            playPcm(err, 44100);
        });
    }

    /**
     * Plays alert/notification chime (e.g. rematch request, opponent joined).
     */
    public void playNotification() {
        if (muted) return;
        soundExecutor.submit(() -> {
            int[] notes = {659, 880}; // E5, A5
            byte[] notif = synthesizeArpeggio(notes, 0.10f, 0.2f, 0.6f);
            playPcm(notif, 44100);
        });
    }

    // --- Waveform Synthesis Helpers ---

    private byte[] synthesizeTone(float freq, float durationSec, float volume, boolean squareish) {
        int sampleRate = 44100;
        int totalSamples = (int) (sampleRate * durationSec);
        byte[] buffer = new byte[totalSamples * 2]; // 16-bit mono

        for (int i = 0; i < totalSamples; i++) {
            double time = (double) i / sampleRate;
            double angle = 2.0 * Math.PI * freq * time;
            double sample = Math.sin(angle);

            if (squareish) {
                // Add harmonic warmth
                sample = 0.7 * Math.sin(angle) + 0.3 * Math.sin(3 * angle);
            }

            // Exponential decay envelope
            double envelope = Math.exp(-3.0 * (time / durationSec));
            short val = (short) (sample * envelope * volume * Short.MAX_VALUE);

            buffer[2 * i] = (byte) ((val >> 8) & 0xFF);     // High byte
            buffer[2 * i + 1] = (byte) (val & 0xFF);          // Low byte
        }
        return buffer;
    }

    private byte[] synthesizeToneSweep(float startFreq, float endFreq, float durationSec, float volume) {
        int sampleRate = 44100;
        int totalSamples = (int) (sampleRate * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        double phase = 0.0;
        for (int i = 0; i < totalSamples; i++) {
            double fraction = (double) i / totalSamples;
            double currentFreq = startFreq + (endFreq - startFreq) * fraction;
            phase += 2.0 * Math.PI * currentFreq / sampleRate;

            double sample = Math.sin(phase);
            double envelope = Math.sin(Math.PI * fraction); // smooth attack & release
            short val = (short) (sample * envelope * volume * Short.MAX_VALUE);

            buffer[2 * i] = (byte) ((val >> 8) & 0xFF);
            buffer[2 * i + 1] = (byte) (val & 0xFF);
        }
        return buffer;
    }

    private byte[] synthesizeDualTone(float freq1, float freq2, float durationSec, float volume) {
        int sampleRate = 44100;
        int totalSamples = (int) (sampleRate * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double time = (double) i / sampleRate;
            double sample = 0.5 * Math.sin(2.0 * Math.PI * freq1 * time)
                          + 0.5 * Math.sin(2.0 * Math.PI * freq2 * time);
            double envelope = Math.exp(-2.5 * (time / durationSec));
            short val = (short) (sample * envelope * volume * Short.MAX_VALUE);

            buffer[2 * i] = (byte) ((val >> 8) & 0xFF);
            buffer[2 * i + 1] = (byte) (val & 0xFF);
        }
        return buffer;
    }

    private byte[] synthesizeArpeggio(int[] frequencies, float noteDurationSec, float finalDecaySec, float volume) {
        int sampleRate = 44100;
        int totalNoteSamples = (int) (sampleRate * noteDurationSec);
        int finalDecaySamples = (int) (sampleRate * finalDecaySec);
        int totalSamples = totalNoteSamples * (frequencies.length - 1) + (totalNoteSamples + finalDecaySamples);

        byte[] buffer = new byte[totalSamples * 2];

        int currentSampleOffset = 0;
        for (int n = 0; n < frequencies.length; n++) {
            float freq = frequencies[n];
            int noteLen = (n == frequencies.length - 1) ? (totalNoteSamples + finalDecaySamples) : totalNoteSamples;
            double noteDuration = (double) noteLen / sampleRate;

            for (int i = 0; i < noteLen; i++) {
                int overallIndex = currentSampleOffset + i;
                if (overallIndex >= totalSamples) break;

                double time = (double) i / sampleRate;
                double sample = Math.sin(2.0 * Math.PI * freq * time);
                double envelope = Math.exp(-2.0 * (time / noteDuration));
                short val = (short) (sample * envelope * volume * Short.MAX_VALUE);

                buffer[2 * overallIndex] = (byte) ((val >> 8) & 0xFF);
                buffer[2 * overallIndex + 1] = (byte) (val & 0xFF);
            }
            currentSampleOffset += totalNoteSamples;
        }

        return buffer;
    }

    private void playPcm(byte[] audioData, int sampleRate) {
        try {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                return;
            }
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, audioData, 0, audioData.length);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception e) {
            // Audio line not available on some headless environments; ignore gracefully
        }
    }
}
