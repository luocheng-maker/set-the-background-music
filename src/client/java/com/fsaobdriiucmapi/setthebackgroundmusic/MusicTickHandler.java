package com.fsaobdriiucmapi.setthebackgroundmusic;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicTickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicTickHandler");
    private static final int MAX_RETRY_ATTEMPTS = 30;
    private static final int RETRY_DELAY_TICKS = 20;
    private static final int REPLAY_DELAY_TICKS = 40;

    private final MusicPlayer player;
    private boolean started = false;
    private boolean musicDisabled = false;
    private boolean volumeApplied = false;
    private int retryCount = 0;
    private int retryDelayCounter = 0;
    private int replayDelayTicks = 0;

    /** 上一次看到的 SoundManager 实例，用于检测 SoundEngine 重启 */
    private Object lastSoundManager = null;

    public MusicTickHandler(MusicPlayer player) {
        this.player = player;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.options == null) return;

            // ===== SoundEngine 重启检测 =====
            Object sm = client.getSoundManager();
            if (lastSoundManager == null) {
                lastSoundManager = sm;
            } else if (lastSoundManager != sm) {
                LOGGER.info("Sound engine restarted, resetting audio engines.");
                AudioPlayer.resetAfterSoundEngineRestart();
                lastSoundManager = sm;
                if (started) {
                    replayDelayTicks = REPLAY_DELAY_TICKS;
                }
            }

            if (!musicDisabled) {
                client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0D);
                musicDisabled = true;
                LOGGER.info("Vanilla music disabled.");
            }

            if (!volumeApplied) {
                AudioPlayer.setGlobalVolume(ConfigManager.get().volume);
                volumeApplied = true;
            }

            // 资源重载 / SoundEngine 重启后，等 SoundEngine 就绪再重播
            if (replayDelayTicks > 0) {
                replayDelayTicks--;
                if (replayDelayTicks == 0) {
                    LOGGER.info("Re-playing current track after sound engine restart.");
                    player.playIndex(player.getCurrentIndex());
                }
                return;
            }

            if (started) {
                if (!player.isSingleSong() && AudioPlayer.isIdle()) {
                    if (MelodyPlayer.consumeClipFailure()) {
                        LOGGER.info("Melody clip invalidated (OpenAL restart?), scheduling replay.");
                        replayDelayTicks = REPLAY_DELAY_TICKS;
                    } else {
                        player.playNext();
                    }
                }
                return;
            }

            if (MelodyPlayer.isLoading() || JavaFXMediaPlayer.isLoading()) return;
            if (AudioPlayer.isPlaying()) {
                started = true;
                LOGGER.info("Music playback started successfully.");
                return;
            }

            if (retryCount >= MAX_RETRY_ATTEMPTS) {
                started = true;
                LOGGER.error("Max retry attempts reached ({}), giving up.", MAX_RETRY_ATTEMPTS);
                return;
            }

            if (retryDelayCounter > 0) {
                retryDelayCounter--;
                return;
            }

            attemptPlay();
            retryDelayCounter = RETRY_DELAY_TICKS;
        });
    }

    private void attemptPlay() {
        if (started) return;
        if (retryCount >= MAX_RETRY_ATTEMPTS) return;

        if (retryCount == 0) {
            player.playNext();
        } else {
            player.playIndex(player.getCurrentIndex());
        }

        retryCount++;
        LOGGER.info("Attempting music playback (attempt {}/{}).", retryCount, MAX_RETRY_ATTEMPTS);
    }
}