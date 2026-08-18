package com.fsaobdriiucmapi.setthebackgroundmusic;

import de.keksuccino.melody.resources.audio.SimpleAudioFactory;
import de.keksuccino.melody.resources.audio.SimpleAudioFactory.SourceType;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALException;
import de.keksuccino.melody.resources.audio.MelodyAudioException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class MelodyPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MelodyPlayer");
    private static ALAudioClip currentClip;
    private static boolean isLoading = false;
    private static boolean loopSingle = false;
    private static float globalVolume = 0.5f;

    public static void setLoopSingle(boolean loop) {
        loopSingle = loop;
    }

    public static void setGlobalVolume(float volume) {
        globalVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (currentClip != null) {
            try {
                currentClip.setVolume(globalVolume);
            } catch (Exception e) {
                LOGGER.warn("Error setting volume: {}", e.getMessage());
            }
        }
    }

    public static void play(Path audioFile) {
        if (isLoading) {
            LOGGER.warn("Already loading audio, ignoring: {}", audioFile.getFileName());
            return;
        }

        String filePath = audioFile.toAbsolutePath().toString();
        String fileName = audioFile.getFileName().toString().toLowerCase();
        CompletableFuture<ALAudioClip> future;

        try {
            if (fileName.endsWith(".ogg")) {
                future = SimpleAudioFactory.ogg(filePath, SourceType.LOCAL_FILE);
            } else if (fileName.endsWith(".wav")) {
                future = SimpleAudioFactory.wav(filePath, SourceType.LOCAL_FILE);
            } else {
                LOGGER.warn("Unsupported audio format: {}", fileName);
                return;
            }
        } catch (MelodyAudioException e) {
            LOGGER.error("Failed to load audio: {}", audioFile, e);
            return;
        }

        isLoading = true;

        future.thenAccept(clip -> {
            stop();
            currentClip = clip;
            try {
                clip.setVolume(globalVolume);
                if (loopSingle) {
                    clip.setLooping(true);
                }
                clip.play();
                LOGGER.info("Now playing: {} (volume={}%)", audioFile.getFileName(), Math.round(globalVolume * 100));

                // Toast 弹窗
                String title = audioFile.getFileName().toString();
                title = title.replaceFirst("\\.(ogg|wav)$", "");
                try {
                    SystemToast.addOrUpdate(
                        Minecraft.getInstance().getToastManager(),
                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal("🎵 " + title),
                        Component.literal("音量: " + Math.round(globalVolume * 100) + "%")
                    );
                    LOGGER.info("Toast displayed for: {}", title);
                } catch (Exception e) {
                    LOGGER.warn("Failed to show toast: {}", e.getMessage());
                }

            } catch (ALException e) {
                LOGGER.error("Failed to play audio", e);
            } finally {
                isLoading = false;
            }
        }).exceptionally(e -> {
            LOGGER.error("Failed to load audio: {}", audioFile, e);
            isLoading = false;
            return null;
        });
    }

    public static void stop() {
        if (currentClip != null) {
            try {
                currentClip.stop();
                currentClip.close();
            } catch (Exception e) {
                LOGGER.warn("Error stopping audio", e);
            } finally {
                currentClip = null;
            }
            LOGGER.info("Stopped audio.");
        }
    }

    public static void setVolume(float volume) {
        if (currentClip != null) {
            try {
                currentClip.setVolume(volume);
            } catch (Exception e) {
                LOGGER.warn("Error setting volume", e);
            }
        }
    }

    public static boolean isPlaying() {
        if (currentClip == null) return false;
        try {
            return currentClip.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isIdle() {
        return !isLoading && !isPlaying();
    }

    public static void pause() {
        if (currentClip != null) {
            try {
                currentClip.pause();
            } catch (Exception e) {
                LOGGER.warn("Error pausing audio", e);
            }
        }
    }

    public static void resume() {
        if (currentClip != null) {
            try {
                currentClip.resume();
            } catch (Exception e) {
                LOGGER.warn("Error resuming audio", e);
            }
        }
    }

    public static void setLooping(boolean looping) {
        if (currentClip != null) {
            try {
                currentClip.setLooping(looping);
            } catch (Exception e) {
                LOGGER.warn("Error setting loop", e);
            }
        }
    }
}