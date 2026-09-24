package com.fsaobdriiucmapi.setthebackgroundmusic;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

public class JavaFXMediaPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("JavaFXMediaPlayer");
    private static volatile MediaPlayer currentPlayer;
    private static volatile String currentRequestedUri = null;
    private static volatile float globalVolume = 0.5f;
    private static volatile boolean loopSingle = false;
    private static Runnable onEndOfMediaCallback;
    private static Runnable fallbackCallback;
    private static volatile boolean loading = false;

    public static void setLoopSingle(boolean loop) { loopSingle = loop; }
    public static void setOnEndOfMedia(Runnable callback) { onEndOfMediaCallback = callback; }

    public static void setGlobalVolume(float volume) {
        globalVolume = Math.max(0.0f, Math.min(1.0f, volume));
        MediaPlayer p = currentPlayer;
        if (p != null) {
            try { p.setVolume(globalVolume); }
            catch (Exception e) { LOGGER.warn("Error setting JavaFX volume", e); }
        }
    }

    public static void play(Path audioFile, Runnable onFallback) {
        String uri = audioFile.toFile().toURI().toString();

        synchronized (JavaFXMediaPlayer.class) {
            if (uri.equals(currentRequestedUri) && (loading || isPlaying())) {
                LOGGER.debug("JavaFX already handling: {}", audioFile.getFileName());
                return;
            }
            currentRequestedUri = uri;
        }

        fallbackCallback = onFallback;
        loading = true;

        JavaFXHelper.runLater(() -> {
            try {
                MediaPlayer old = currentPlayer;
                currentPlayer = null;
                if (old != null) {
                    try { old.stop(); old.dispose(); } catch (Exception ignored) { }
                }

                File file = audioFile.toFile();
                Media media = new Media(file.toURI().toString());
                MediaPlayer player = new MediaPlayer(media);
                currentPlayer = player;

                player.setVolume(globalVolume);

                player.setOnError(() -> {
                    LOGGER.error("JavaFX MediaPlayer error: {}", player.getError().getMessage());
                    loading = false;
                    stop();
                    if (fallbackCallback != null) fallbackCallback.run();
                });

                player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                    if (newStatus == MediaPlayer.Status.PLAYING
                            || newStatus == MediaPlayer.Status.STOPPED
                            || newStatus == MediaPlayer.Status.HALTED) {
                        loading = false;
                    }
                });

                if (loopSingle) {
                    player.setCycleCount(MediaPlayer.INDEFINITE);
                } else {
                    player.setCycleCount(1);
                    player.setOnEndOfMedia(() -> {
                        if (onEndOfMediaCallback != null) onEndOfMediaCallback.run();
                    });
                }

                player.play();
                LOGGER.info("JavaFX playing: {}", audioFile.getFileName());
            } catch (Exception e) {
                LOGGER.error("Failed to init JavaFX MediaPlayer", e);
                loading = false;
                stop();
                if (fallbackCallback != null) fallbackCallback.run();
            }
        });
    }

    public static void stop() {
        MediaPlayer p = currentPlayer;
        currentPlayer = null;
        currentRequestedUri = null;
        loading = false;
        if (p == null) return;

        Runnable disposeTask = () -> {
            try { p.stop(); p.dispose(); }
            catch (Exception e) { LOGGER.warn("Error disposing JavaFX player: {}", e.getMessage()); }
        };

        try {
            if (Platform.isFxApplicationThread()) {
                disposeTask.run();
            } else {
                Platform.runLater(disposeTask);
            }
        } catch (Exception e) {
            LOGGER.warn("Error scheduling JavaFX dispose: {}", e.getMessage());
        }
    }

    /**
     * SoundEngine 重启后调用：清引用不 dispose。
     * 此时底层媒体已失效，dispose 可能抛异常，直接丢弃即可。
     */
    public static void invalidate() {
        currentPlayer = null;
        currentRequestedUri = null;
        loading = false;
    }

    public static void pause() {
        MediaPlayer p = currentPlayer;
        if (p == null) return;
        try {
            MediaPlayer.Status status = p.getStatus();
            // 只在 PLAYING / PAUSED 状态才调 pause，避免底层 gstMedia 为 null 时 NPE
            if (status == MediaPlayer.Status.PLAYING || status == MediaPlayer.Status.PAUSED) {
                p.pause();
            }
        } catch (Exception e) {
            LOGGER.warn("pause error: {}", e.getMessage());
        }
    }

    public static void resume() {
        MediaPlayer p = currentPlayer;
        if (p == null) return;
        try {
            MediaPlayer.Status status = p.getStatus();
            if (status == MediaPlayer.Status.PAUSED
                    || status == MediaPlayer.Status.READY
                    || status == MediaPlayer.Status.STALLED) {
                p.play();
            }
        } catch (Exception e) {
            LOGGER.warn("resume error: {}", e.getMessage());
        }
    }

    public static boolean isPlaying() {
        MediaPlayer p = currentPlayer;
        if (p == null) return false;
        try { return p.getStatus() == MediaPlayer.Status.PLAYING; }
        catch (Exception e) { return false; }
    }

    public static boolean isLoading() { return loading; }
}