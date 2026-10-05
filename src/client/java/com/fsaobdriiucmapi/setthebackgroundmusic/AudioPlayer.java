package com.fsaobdriiucmapi.setthebackgroundmusic;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AudioPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AudioPlayer");
    private static final ScheduledExecutorService FADE_EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MusicFade");
            t.setDaemon(true);
            return t;
        });

    // ===== P2-1：后端列表（按优先级）=====
    private static final MelodyBackend MELODY = new MelodyBackend();
    private static final JavaFXBackend JAVAFX = new JavaFXBackend();
    private static final List<AudioBackend> BACKENDS = List.of(MELODY, JAVAFX);

    private static ScheduledFuture<?> currentFadeTask;
    private static volatile float currentVolume = 0.5f;
    private static final AtomicBoolean fading = new AtomicBoolean(false);
    private static volatile long lastPlayRequestMs = 0L;
    // P3-1：从 3s → 5s，给大文件/慢解码留余量
    private static final long STARTUP_GRACE_MS = 5000L;

    private static volatile Consumer<Path> onPlaybackFailed = null;
    private static final AtomicBoolean failureNotified = new AtomicBoolean(false);
    private static volatile Path currentFile = null;
    private static final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    public static void setOnPlaybackFailed(Consumer<Path> listener) {
        onPlaybackFailed = listener;
    }

    static void notifyPlaybackFailed(Path file) {
        if (file == null || currentFile == null || !currentFile.equals(file)) return;
        if (!failureNotified.compareAndSet(false, true)) return;

        int count = consecutiveFailures.incrementAndGet();
        LOGGER.warn("Both engines failed for: {} (consecutive: {})", file.getFileName(), count);

        if (count >= MAX_CONSECUTIVE_FAILURES) {
            LOGGER.error("Too many consecutive failures ({}), stopping auto-skip.", MAX_CONSECUTIVE_FAILURES);
            return;
        }

        Consumer<Path> listener = onPlaybackFailed;
        if (listener != null) {
            Minecraft.getInstance().execute(() -> listener.accept(file));
        }
    }

    static void notifyPlaybackSuccess(Path file) {
        if (file != null && currentFile != null && currentFile.equals(file)) {
            failureNotified.set(false);
            consecutiveFailures.set(0);
        }
    }

    private static AudioBackend selectBackend(String lowerFileName) {
        for (AudioBackend b : BACKENDS) {
            if (b.canPlay(lowerFileName)) return b;
        }
        return null;
    }

    public static void play(Path audioFile) {
        lastPlayRequestMs = System.currentTimeMillis();
        currentFile = audioFile;
        failureNotified.set(false);

        boolean doFade = ConfigManager.get().fadeEnabled;
        int dur = ConfigManager.get().fadeDurationMs;
        float targetVol = ConfigManager.get().volume;

        Runnable doPlay = () -> {
            String lowerName = audioFile.getFileName().toString().toLowerCase();
            AudioBackend backend = selectBackend(lowerName);
            if (backend == null) {
                LOGGER.warn("No backend can play: {}", lowerName);
                notifyPlaybackFailed(audioFile);
                return;
            }

            Runnable fallback = null;
            if (backend == JAVAFX) {
                fallback = () -> {
                    LOGGER.warn("JavaFX playback failed, falling back to Melody for: {}", lowerName);
                    Minecraft.getInstance().execute(() -> MELODY.play(audioFile, null));
                };
            }

            LOGGER.info("Playing via {}: {}", backend.name(), lowerName);
            backend.play(audioFile, fallback);
        };

        if (currentFadeTask != null && !currentFadeTask.isDone()) currentFadeTask.cancel(false);

        if (doFade && isPlaying()) {
            fadeTo(0f, dur, () -> {
                stop();
                doPlay.run();
                FADE_EXECUTOR.schedule(
                    () -> fadeTo(targetVol, dur, null),
                    300, TimeUnit.MILLISECONDS);
            });
        } else {
            stop();
            doPlay.run();
            if (doFade) {
                currentVolume = 0f;
                applyVolume(0f);
                FADE_EXECUTOR.schedule(
                    () -> fadeTo(targetVol, dur, null),
                    300, TimeUnit.MILLISECONDS);
            } else {
                currentVolume = targetVol;
                applyVolume(targetVol);
            }
        }
    }

    public static synchronized void fadeTo(float targetVolume, int durationMs, Runnable onComplete) {
        if (currentFadeTask != null && !currentFadeTask.isDone()) {
            currentFadeTask.cancel(false);
        }
        if (durationMs <= 0) {
            currentVolume = targetVolume;
            applyVolume(targetVolume);
            if (onComplete != null) Minecraft.getInstance().execute(onComplete);
            return;
        }

        final float startVolume = currentVolume;
        final float endVolume = Math.max(0f, Math.min(1f, targetVolume));
        final long startTime = System.nanoTime();
        final long durationNs = durationMs * 1_000_000L;
        fading.set(true);

        currentFadeTask = FADE_EXECUTOR.scheduleAtFixedRate(() -> {
            long elapsed = System.nanoTime() - startTime;
            float t = Math.min(1.0f, (float) elapsed / durationNs);
            float vol = startVolume + (endVolume - startVolume) * t;
            applyVolume(vol);
            currentVolume = vol;

            if (t >= 1.0f) {
                fading.set(false);
                if (currentFadeTask != null) currentFadeTask.cancel(false);
                if (onComplete != null) {
                    Minecraft.getInstance().execute(onComplete);
                }
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    public static void applyVolume(float v) {
        for (AudioBackend b : BACKENDS) b.setVolume(v);
    }

    public static float getCurrentVolume() {
        return currentVolume;
    }

    public static void stop() {
        if (currentFadeTask != null && !currentFadeTask.isDone()) currentFadeTask.cancel(false);
        for (AudioBackend b : BACKENDS) b.stop();
    }

    public static void resetAfterSoundEngineRestart() {
        try {
            for (AudioBackend b : BACKENDS) b.invalidate();
            currentVolume = ConfigManager.get().volume;
            LOGGER.info("Audio engines reset after SoundEngine restart.");
        } catch (Throwable t) {
            LOGGER.warn("Error resetting audio engines", t);
        }
    }

    public static void pause() {
        for (AudioBackend b : BACKENDS) b.pause();
    }

    public static void resume() {
        for (AudioBackend b : BACKENDS) b.resume();
    }

    public static void setGlobalVolume(float volume) {
        float v = Math.max(0f, Math.min(1f, volume));
        currentVolume = v;
        applyVolume(v);
    }

    public static void setLoopSingle(boolean loop) {
        for (AudioBackend b : BACKENDS) b.setLoopSingle(loop);
    }

    public static boolean isPlaying() {
        for (AudioBackend b : BACKENDS) if (b.isPlaying()) return true;
        return false;
    }

    /**
     * P3-1：判断"当前确实没在出声、也没在加载"。
     * 判断顺序调整：loading > playing > grace，避免大文件误判 idle 提前切歌。
     */
    public static boolean isIdle() {
        for (AudioBackend b : BACKENDS) if (b.isLoading()) return false;
        if (isPlaying()) return false;
        if (System.currentTimeMillis() - lastPlayRequestMs < STARTUP_GRACE_MS) return false;
        return true;
    }

    public static boolean isFading() {
        return fading.get();
    }

    /** 供 MusicTickHandler 使用：任一后端报告失效。 */
    public static boolean consumeAnyFailure() {
        boolean any = false;
        for (AudioBackend b : BACKENDS) {
            if (b.consumeFailure()) any = true;
        }
        return any;
    }
}