package com.fsaobdriiucmapi.setthebackgroundmusic;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicHud {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicHud");
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("setthebackgroundmusic", "music_hud");

    private static volatile long showUntilMs = 0L;

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, new HudElement() {
            @Override
            public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker delta) {
                render(extractor);
            }
        });
        LOGGER.info("Music HUD registered.");
    }

    private static void render(GuiGraphicsExtractor extractor) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.hudEnabled) return;
        if (!AudioPlayer.isPlaying()) return;

        long now = System.currentTimeMillis();
        if (now > showUntilMs) return;

        MusicPlayer player = MyMusicMod.getPlayer();
        if (player == null) return;

        String track = player.getCurrentTrackName();
        if (track.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = extractor.guiWidth();

        String line = "🎵 " + track;
        String volLine = "音量 " + Math.round(AudioPlayer.getCurrentVolume() * 100) + "%"
                + (player.isCurrentFavorite() ? "  ★" : "");

        int textWidth = mc.font.width(line);
        int volWidth = mc.font.width(volLine);
        int boxWidth = Math.max(textWidth, volWidth) + 20;
        int boxHeight = 34;
        int x = screenWidth - boxWidth - 10;
        int y = 10;

        extractor.fill(x, y, x + boxWidth, y + boxHeight, 0x99000000);
        extractor.text(mc.font, line, x + 10, y + 6, 0xFFFFFF00);
        extractor.text(mc.font, volLine, x + 10, y + 18, 0xFFFFFFFF);
    }

    public static void notifyTrackChanged(String trackName) {
        if (trackName == null || trackName.isEmpty()) return;
        int seconds = ConfigManager.get().hudDisplaySeconds;
        showUntilMs = System.currentTimeMillis() + seconds * 1000L;
    }
}