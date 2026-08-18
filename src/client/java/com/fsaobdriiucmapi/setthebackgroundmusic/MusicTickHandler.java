package com.fsaobdriiucmapi.setthebackgroundmusic;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicTickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicTickHandler");

    private final MusicPlayer player;
    private boolean started = false;
    private boolean musicDisabled = false;
    private int tickCounter = 0;
    private boolean volumeApplied = false;

    public MusicTickHandler(MusicPlayer player) {
        this.player = player;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 1. 禁用原版音乐
            if (!musicDisabled && client.options != null) {
                client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0D);
                musicDisabled = true;
                LOGGER.info("Vanilla music disabled.");
            }

            // 2. 等待 SoundManager 就绪
            if (client.getSoundManager() == null) return;

            // 3. 应用配置音量（确保 Melody 使用配置音量）
            if (!volumeApplied) {
                MelodyPlayer.setGlobalVolume(ConfigManager.get().volume);
                volumeApplied = true;
            }

            // 4. 等待 20 帧
            if (tickCounter < 20) {
                tickCounter++;
                return;
            }

            // 5. 首次播放
            if (!started) {
                started = true;
                player.playNext();
                LOGGER.info("Started music playback.");
                return;
            }

            // 6. 多首模式自动切歌
            if (!player.isSingleSong() && MelodyPlayer.isIdle()) {
                player.playNext();
            }
        });
    }
}