package com.fsaobdriiucmapi.setthebackgroundmusic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MusicFileScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicFileScanner");

    public static List<Path> scan() {
        List<Path> musicFiles = new ArrayList<>();
        Path musicDir = Paths.get("config", MyMusicMod.MOD_ID, "music");

        if (!Files.exists(musicDir)) {
            try {
                Files.createDirectories(musicDir);
                LOGGER.info("Created music directory: {}", musicDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create music directory", e);
                return musicFiles;
            }
        }

        try {
            Files.walk(musicDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".ogg"))
                    .forEach(musicFiles::add);
            LOGGER.info("Found {} .ogg files.", musicFiles.size());
            for (Path p : musicFiles) {
                LOGGER.info("  - {}", p.getFileName());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan music directory", e);
        }

        return musicFiles;
    }
}