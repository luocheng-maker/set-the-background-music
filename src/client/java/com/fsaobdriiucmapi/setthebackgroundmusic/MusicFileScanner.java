package com.fsaobdriiucmapi.setthebackgroundmusic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MusicFileScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicFileScanner");

    /**
     * 全部支持的音频扩展名。
     * 1. Melody 原生：.ogg / .wav
     * 2. JavaFX / Java Sound 原生：.mp3 / .m4a / .aiff / .aif / .au
     * 3. FFmpeg 转码：其余全部
     */
    public static final Set<String> SUPPORTED_EXT = Set.of(
            ".ogg", ".wav",
            ".mp3", ".m4a", ".aiff", ".aif", ".aifc", ".au",
            ".flac", ".opus", ".wma", ".aac", ".ape", ".wv", ".mka",
            ".m4b", ".m4p", ".caf", ".amr", ".mp2",
            ".ac3", ".eac3", ".dts", ".tta",
            ".rm", ".ra", ".voc",
            ".webm", ".weba", ".mkv",
            ".3gp", ".3g2"
    );

    /** 与 SUPPORTED_EXT 保持一致的正则（不带点） */
    public static final String EXT_REGEX =
            "\\.(ogg|wav|mp3|m4a|aiff|aif|aifc|au|flac|opus|wma|aac|ape|wv|mka"
            + "|m4b|m4p|caf|amr|mp2|ac3|eac3|dts|tta|rm|ra|voc|webm|weba|mkv|3gp|3g2)$";

    public static Path getMusicDir() {
        return Paths.get("config", MyMusicMod.MOD_ID, "music");
    }

    public static void ensureDir() {
        Path dir = getMusicDir();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                LOGGER.info("Created music directory: {}", dir);
            } catch (IOException e) {
                LOGGER.error("Failed to create music directory", e);
            }
        }
    }

    public static List<Path> scan() {
        return scanCategory(null);
    }

    public static List<Path> scanCategory(String category) {
        ensureDir();
        List<Path> result = new ArrayList<>();
        Path root = getMusicDir();
        Path base = (category == null || category.isEmpty()) ? root : root.resolve(category);

        if (!Files.exists(base)) {
            LOGGER.warn("Category path does not exist: {}", base);
            return result;
        }

        try {
            Files.walk(base)
                .filter(Files::isRegularFile)
                .filter(MusicFileScanner::isSupportedAudio)
                .forEach(result::add);
        } catch (IOException e) {
            LOGGER.error("Failed to scan music directory", e);
        }

        LOGGER.info("Found {} music files in category '{}'", result.size(),
                category == null || category.isEmpty() ? "(all)" : category);
        for (Path p : result) {
            LOGGER.info("  - {}", p.getFileName());
        }
        return result;
    }

    public static List<String> listCategories() {
        ensureDir();
        Path root = getMusicDir();
        List<String> cats = new ArrayList<>();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                  .forEach(p -> cats.add(p.getFileName().toString()));
        } catch (IOException e) {
            LOGGER.error("Failed to list categories", e);
        }
        Collections.sort(cats);
        return cats;
    }

    private static boolean isSupportedAudio(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        for (String ext : SUPPORTED_EXT) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }
}