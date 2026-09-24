package com.fsaobdriiucmapi.setthebackgroundmusic;

import de.keksuccino.melody.resources.audio.SimpleAudioFactory;
import de.keksuccino.melody.resources.audio.SimpleAudioFactory.SourceType;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MelodyPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MelodyPlayer");
    private static ALAudioClip currentClip;
    private static volatile boolean isLoading = false;
    private static boolean loopSingle = false;
    private static float globalVolume = 0.5f;

    private static volatile boolean lastStopWasFailure = false;

    private static final ExecutorService TRANSCODE_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MusicTranscode");
                t.setDaemon(true);
                return t;
            });

    private static Encoder javeEncoder;

    private static final Set<String> EXTENDED_FORMATS = Set.of(
            ".flac", ".opus", ".wma", ".m4a", ".m4b", ".m4p", ".mp4",
            ".aac", ".ape", ".wv", ".mka",
            ".mp2", ".ac3", ".eac3", ".dts", ".tta",
            ".caf", ".aifc", ".amr",
            ".rm", ".ra", ".voc",
            ".webm", ".weba", ".mkv",
            ".3gp", ".3g2"
    );

    public static void setLoopSingle(boolean loop) { loopSingle = loop; }

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
        isLoading = true;
        lastStopWasFailure = false;

        String fileName = audioFile.getFileName().toString().toLowerCase();
        CompletableFuture<ALAudioClip> future;

        try {
            if (fileName.endsWith(".ogg")) {
                future = SimpleAudioFactory.ogg(audioFile.toAbsolutePath().toString(), SourceType.LOCAL_FILE);

            } else if (fileName.endsWith(".wav")) {
                future = SimpleAudioFactory.wav(audioFile.toAbsolutePath().toString(), SourceType.LOCAL_FILE);

            } else if (fileName.endsWith(".aiff")
                    || fileName.endsWith(".aif")
                    || fileName.endsWith(".au")) {
                future = CompletableFuture
                        .supplyAsync(() -> decodeViaJavaSound(audioFile), TRANSCODE_EXECUTOR)
                        .thenCompose(tempWav -> {
                            if (tempWav == null) {
                                return CompletableFuture.failedFuture(
                                        new RuntimeException("Java Sound decode failed"));
                            }
                            return loadOnRenderThread(tempWav);
                        });

            } else if (isExtendedFormat(fileName)) {
                future = CompletableFuture
                        .supplyAsync(() -> transcodeWithJave2(audioFile), TRANSCODE_EXECUTOR)
                        .thenCompose(tempWav -> {
                            if (tempWav == null) {
                                return CompletableFuture.failedFuture(
                                        new RuntimeException("JAVE2 transcode failed"));
                            }
                            return loadOnRenderThread(tempWav);
                        });

            } else {
                LOGGER.warn("Unsupported audio format: {}", fileName);
                isLoading = false;
                AudioPlayer.notifyPlaybackFailed(audioFile);
                return;
            }
        } catch (Exception e) {
            isLoading = false;

            if (isOpenAlNotReady(e)) {
                LOGGER.info("OpenAL not ready yet, will retry on next tick.");
                return;
            }

            LOGGER.error("Failed to load audio: {}", audioFile, e);
            AudioPlayer.notifyPlaybackFailed(audioFile);
            return;
        }

        future.thenAccept(clip -> {
            stop();
            currentClip = clip;
            lastStopWasFailure = false;
            try {
                clip.setVolume(globalVolume);
                if (loopSingle) clip.setLooping(true);
                clip.play();
                LOGGER.info("Now playing: {} (volume={}%)",
                        audioFile.getFileName(), Math.round(globalVolume * 100));
                AudioPlayer.notifyPlaybackSuccess(audioFile);

                String title = audioFile.getFileName().toString()
                        .replaceFirst(MusicFileScanner.EXT_REGEX, "");
                final float volumeSnapshot = globalVolume;

                Minecraft.getInstance().execute(() -> {
                    try {
                        SystemToast.addOrUpdate(
                            Minecraft.getInstance().getToastManager(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("stbm.toast.now_playing", title),
                            Component.translatable("stbm.toast.volume",
                                    String.valueOf(Math.round(volumeSnapshot * 100)))
                        );
                    } catch (Exception e) {
                        LOGGER.warn("Failed to show toast: {}", e.getMessage());
                    }
                });
            } catch (ALException e) {
                LOGGER.error("Failed to play audio", e);
                AudioPlayer.notifyPlaybackFailed(audioFile);
            } finally {
                isLoading = false;
            }
        }).exceptionally(e -> {
            isLoading = false;

            if (isOpenAlNotReady(e)) {
                LOGGER.info("OpenAL not ready yet, will retry on next tick.");
                return null;
            }

            LOGGER.error("Failed to load audio: {}", audioFile, e);
            AudioPlayer.notifyPlaybackFailed(audioFile);
            return null;
        });
    }

    private static boolean isOpenAlNotReady(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            String msg = cause.getMessage();
            if (msg != null && msg.toLowerCase().contains("openal not ready")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static CompletableFuture<ALAudioClip> loadOnRenderThread(File tempWav) {
        CompletableFuture<CompletableFuture<ALAudioClip>> outer =
                Minecraft.getInstance().submit(() -> {
                    try {
                        return SimpleAudioFactory.wav(
                                tempWav.getAbsolutePath(), SourceType.LOCAL_FILE);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        return outer.thenCompose(inner -> inner);
    }

    private static boolean isExtendedFormat(String fileName) {
        return EXTENDED_FORMATS.contains(getExtension(fileName));
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot);
    }

    private static File decodeViaJavaSound(Path input) {
        File tempWav = null;
        try {
            tempWav = File.createTempFile("mc_bgm_js_" + System.currentTimeMillis(), ".wav");
            tempWav.deleteOnExit();
            try (AudioInputStream in = AudioSystem.getAudioInputStream(input.toFile())) {
                AudioSystem.write(in, AudioFileFormat.Type.WAVE, tempWav);
            }
            return tempWav;
        } catch (Exception e) {
            LOGGER.error("Java Sound decode error for: {}", input, e);
            safeDelete(tempWav);
            return null;
        }
    }

    private static File transcodeWithJave2(Path input) {
        File tempWav = null;
        try {
            tempWav = File.createTempFile("mc_bgm_jave_" + System.currentTimeMillis(), ".wav");
            tempWav.deleteOnExit();

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");
            audio.setBitRate(1411200);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            if (javeEncoder == null) javeEncoder = new Encoder();
            javeEncoder.encode(new MultimediaObject(input.toFile()), tempWav, attrs);

            if (tempWav.exists() && tempWav.length() > 0) {
                LOGGER.info("JAVE2 transcoded {} -> {} ({} bytes)",
                        input.getFileName(), tempWav.getName(), tempWav.length());
                return tempWav;
            }
            LOGGER.warn("JAVE2 produced empty output for: {}", input.getFileName());
            safeDelete(tempWav);
            return null;
        } catch (Exception e) {
            LOGGER.error("JAVE2 transcode error for: {}", input, e);
            safeDelete(tempWav);
            return null;
        }
    }

    private static void safeDelete(File f) {
        if (f != null) {
            try { f.delete(); } catch (Exception ignored) { }
        }
    }

    public static void stop() {
        if (currentClip != null) {
            try {
                currentClip.stop();
                currentClip.close();
            } catch (Exception e) {
                LOGGER.warn("Error stopping audio: {}", e.getMessage());
            } finally {
                currentClip = null;
            }
            LOGGER.info("Stopped audio.");
        }
    }

    public static void invalidateClip() {
        currentClip = null;
        isLoading = false;
        lastStopWasFailure = false;
    }

    public static void setVolume(float volume) {
        if (currentClip != null) {
            try { currentClip.setVolume(volume); }
            catch (Exception e) { LOGGER.warn("Error setting volume", e); }
        }
    }

    public static boolean isPlaying() {
        if (currentClip == null) return false;
        try {
            return currentClip.isPlaying();
        } catch (Exception e) {
            LOGGER.warn("Clip state query failed (OpenAL restart?): {}", e.getMessage());
            lastStopWasFailure = true;
            currentClip = null;
            return false;
        }
    }

    public static boolean consumeClipFailure() {
        boolean v = lastStopWasFailure;
        lastStopWasFailure = false;
        return v;
    }

    public static boolean isIdle() { return !isLoading && !isPlaying(); }
    public static boolean isLoading() { return isLoading; }

    public static void pause() {
        if (currentClip != null) {
            try { currentClip.pause(); }
            catch (Exception e) { LOGGER.warn("Error pausing audio", e); }
        }
    }

    public static void resume() {
        if (currentClip != null) {
            try { currentClip.resume(); }
            catch (Exception e) { LOGGER.warn("Error resuming audio", e); }
        }
    }

    public static void setLooping(boolean looping) {
        if (currentClip != null) {
            try { currentClip.setLooping(looping); }
            catch (Exception e) { LOGGER.warn("Error setting loop", e); }
        }
    }
}