package com.fsaobdriiucmapi.setthebackgroundmusic;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicTickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicTickHandler");
    private static final int MAX_RETRY_ATTEMPTS = 30;
    private static final int RETRY_DELAY_TICKS = 20;

    private final MusicPlayer player;
    private boolean started = false;
    private boolean musicDisabled = false;
    private boolean volumeApplied = false;
    private int retryCount = 0;
    private int retryDelayCounter = 0;

    public MusicTickHandler(MusicPlayer player) {
        this.player = player;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            if (!musicDisabled && client.options != null) {
                client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0D);
                musicDisabled = true;
                LOGGER.info("Vanilla music disabled.");
            }

            if (!volumeApplied) {
                AudioPlayer.setGlobalVolume(ConfigManager.get().volume);
                volumeApplied = true;
            }

            if (started) {
                if (!player.isSingleSong() && AudioPlayer.isIdle()) {
                    player.playNext();
                }
                return;
            }

            // 任一引擎正在加载或已在播放，都不重复触发
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