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
import java.util.concurrent.CompletableFuture;

public class MelodyPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MelodyPlayer");
    private static ALAudioClip currentClip;
    private static boolean isLoading = false;
    private static boolean loopSingle = false;
    private static float globalVolume = 0.5f;

    // ===== JAVE2 编码器（复用，避免重复初始化） =====
    private static Encoder javeEncoder;

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
                // Melody 原生 OGG
                future = SimpleAudioFactory.ogg(filePath, SourceType.LOCAL_FILE);

            } else if (fileName.endsWith(".wav")) {
                // Melody 原生 WAV
                future = SimpleAudioFactory.wav(filePath, SourceType.LOCAL_FILE);

            } else if (fileName.endsWith(".aiff") || fileName.endsWith(".aif") || fileName.endsWith(".au")) {
                // Java Sound 原生支持 → 解码为临时 WAV 后交给 Melody
                File tempWav = File.createTempFile("mc_bgm_js_" + System.currentTimeMillis(), ".wav");
                tempWav.deleteOnExit();
                try (AudioInputStream in = AudioSystem.getAudioInputStream(audioFile.toFile())) {
                    AudioSystem.write(in, AudioFileFormat.Type.WAVE, tempWav);
                }
                filePath = tempWav.getAbsolutePath();
                future = SimpleAudioFactory.wav(filePath, SourceType.LOCAL_FILE);

            } else if (isExtendedFormat(fileName)) {
                // 扩展格式（FLAC/OPUS/WMA/M4A/AAC/APE/WV/MKA）→ JAVE2 (FFmpeg) 转码
                File tempWav = transcodeWithJave2(audioFile);
                if (tempWav == null) {
                    LOGGER.warn("JAVE2 transcode failed for: {}", fileName);
                    AudioPlayer.notifyPlaybackFailed(audioFile);
                    return;
                }
                filePath = tempWav.getAbsolutePath();
                future = SimpleAudioFactory.wav(filePath, SourceType.LOCAL_FILE);

            } else {
                LOGGER.warn("Unsupported audio format: {}", fileName);
                AudioPlayer.notifyPlaybackFailed(audioFile);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load audio: {}", audioFile, e);
            isLoading = false;
            AudioPlayer.notifyPlaybackFailed(audioFile);
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
                AudioPlayer.notifyPlaybackSuccess(audioFile);

                String title = audioFile.getFileName().toString();
                title = title.replaceFirst(MusicFileScanner.EXT_REGEX, "");
                final String toastTitle = title;
                final float volumeSnapshot = globalVolume;

                Minecraft.getInstance().execute(() -> {
                    try {
                        SystemToast.addOrUpdate(
                            Minecraft.getInstance().getToastManager(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.literal("🎵 " + toastTitle),
                            Component.literal("音量: " + Math.round(volumeSnapshot * 100) + "%")
                        );
                        LOGGER.info("Toast displayed for: {}", toastTitle);
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
            LOGGER.error("Failed to load audio: {}", audioFile, e);
            isLoading = false;
            AudioPlayer.notifyPlaybackFailed(audioFile);
            return null;
        });
    }

    // =========================================================
    // 扩展格式判定与 JAVE2 转码
    // =========================================================

    /** 需要 JAVE2 (FFmpeg) 转码的扩展格式 */
    private static boolean isExtendedFormat(String fileName) {
        return fileName.endsWith(".flac")
                || fileName.endsWith(".opus")
                || fileName.endsWith(".wma")
                || fileName.endsWith(".m4a")
                || fileName.endsWith(".mp4")
                || fileName.endsWith(".aac")
                || fileName.endsWith(".ape")
                || fileName.endsWith(".wv")
                || fileName.endsWith(".mka");
    }

    /** 用 JAVE2 把任意音频转成 16-bit PCM WAV 临时文件；失败返回 null */
    private static File transcodeWithJave2(Path input) {
        File tempWav = null;
        try {
            tempWav = File.createTempFile("mc_bgm_jave_" + System.currentTimeMillis(), ".wav");
            tempWav.deleteOnExit();

            // 音频参数：16-bit PCM、44.1kHz、立体声
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");
            audio.setBitRate(1411200);   // 44100 * 16 * 2
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            // 复用 Encoder 实例（Encoder 是线程安全的，可在多个转码任务间共享）
            if (javeEncoder == null) {
                javeEncoder = new Encoder();
            }

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

    // =========================================================
    // 以下方法与之前完全一致，未做任何改动
    // =========================================================

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

    public static boolean isLoading() {
        return isLoading;
    }

    public static void pause() {
        if (currentClip != null) {
            try { currentClip.pause(); } catch (Exception e) { LOGGER.warn("Error pausing audio", e); }
        }
    }

    public static void resume() {
        if (currentClip != null) {
            try { currentClip.resume(); } catch (Exception e) { LOGGER.warn("Error resuming audio", e); }
        }
    }

    public static void setLooping(boolean looping) {
        if (currentClip != null) {
            try { currentClip.setLooping(looping); } catch (Exception e) { LOGGER.warn("Error setting loop", e); }
        }
    }
}