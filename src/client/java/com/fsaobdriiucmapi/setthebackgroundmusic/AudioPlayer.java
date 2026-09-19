package com.fsaobdriiucmapi.setthebackgroundmusic;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
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

    private static ScheduledFuture<?> currentFadeTask;
    private static volatile float currentVolume = 0.5f;
    private static final AtomicBoolean fading = new AtomicBoolean(false);
    private static volatile long lastPlayRequestMs = 0L;
    private static final long STARTUP_GRACE_MS = 3000L;

    // ===== 总失败回调 =====
    private static volatile Consumer<Path> onPlaybackFailed = null;
    private static final AtomicBoolean failureNotified = new AtomicBoolean(false);
    private static volatile Path currentFile = null;
    private static final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    public static void setOnPlaybackFailed(Consumer<Path> listener) {
        onPlaybackFailed = listener;
    }

    /** 两个引擎都失败时调用。同一首歌只触发一次。 */
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

    /** 成功播放时调用，重置失败计数。 */
    static void notifyPlaybackSuccess(Path file) {
        if (file != null && currentFile != null && currentFile.equals(file)) {
            failureNotified.set(false);
            consecutiveFailures.set(0);
        }
    }

    public static void play(Path audioFile) {
        lastPlayRequestMs = System.currentTimeMillis();
        currentFile = audioFile;
        failureNotified.set(false);

        boolean doFade = ConfigManager.get().fadeEnabled;
        int dur = ConfigManager.get().fadeDurationMs;
        float targetVol = ConfigManager.get().volume;

        Runnable doPlay = () -> {
            String fileName = audioFile.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".ogg") || fileName.endsWith(".wav")) {
                LOGGER.info("Playing {} via Melody: {}",
                        fileName.endsWith(".ogg") ? "OGG" : "WAV", fileName);
                MelodyPlayer.play(audioFile);
            } else {
                Runnable fallback = () -> {
                    LOGGER.warn("JavaFX playback failed, falling back to Melody for: {}", fileName);
                    Minecraft.getInstance().execute(() -> MelodyPlayer.play(audioFile));
                };
                LOGGER.info("Attempting playback via JavaFX: {}", fileName);
                JavaFXMediaPlayer.play(audioFile, fallback);
            }
        };

        if (currentFadeTask != null && !currentFadeTask.isDone()) currentFadeTask.cancel(false);

        if (doFade && isPlaying()) {
            fadeTo(0f, dur, () -> {
                JavaFXMediaPlayer.stop();
                MelodyPlayer.stop();
                doPlay.run();
                FADE_EXECUTOR.schedule(
                    () -> fadeTo(targetVol, dur, null),
                    300, TimeUnit.MILLISECONDS);
            });
        } else {
            JavaFXMediaPlayer.stop();
            MelodyPlayer.stop();
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
        MelodyPlayer.setGlobalVolume(v);
        JavaFXMediaPlayer.setGlobalVolume(v);
    }

    public static float getCurrentVolume() {
        return currentVolume;
    }

    public static void stop() {
        if (currentFadeTask != null && !currentFadeTask.isDone()) currentFadeTask.cancel(false);
        JavaFXMediaPlayer.stop();
        MelodyPlayer.stop();
    }

    public static void pause() {
        JavaFXMediaPlayer.pause();
        MelodyPlayer.pause();
    }

    public static void resume() {
        JavaFXMediaPlayer.resume();
        MelodyPlayer.resume();
    }

    public static void setGlobalVolume(float volume) {
        float v = Math.max(0f, Math.min(1f, volume));
        currentVolume = v;
        applyVolume(v);
    }

    public static void setLoopSingle(boolean loop) {
        JavaFXMediaPlayer.setLoopSingle(loop);
        MelodyPlayer.setLoopSingle(loop);
    }

    public static boolean isPlaying() {
        return JavaFXMediaPlayer.isPlaying() || MelodyPlayer.isPlaying();
    }

    public static boolean isIdle() {
        if (System.currentTimeMillis() - lastPlayRequestMs < STARTUP_GRACE_MS) return false;
        return !isPlaying() && !MelodyPlayer.isLoading() && !JavaFXMediaPlayer.isLoading();
    }

    public static boolean isFading() {
        return fading.get();
    }
}