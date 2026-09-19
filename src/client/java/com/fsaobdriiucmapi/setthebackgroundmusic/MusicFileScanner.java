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

    public static final Set<String> SUPPORTED_EXT = Set.of(
            ".ogg", ".wav",
            ".mp3", ".m4a", ".aiff", ".aif", ".au",
            ".flac", ".opus", ".wma", ".aac", ".ape", ".wv", ".mka"
    );

    /** 用于字符串 replaceFirst 的正则，与 SUPPORTED_EXT 保持一致 */
    public static final String EXT_REGEX =
            "\\.(ogg|wav|mp3|m4a|aiff|aif|au|flac|opus|wma|aac|ape|wv|mka)$";

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

    /** 递归扫描全部音乐 */
    public static List<Path> scan() {
        return scanCategory(null);
    }

    /** 递归扫描指定分类（子文件夹名）。null/空 = 全部 */
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

    /** 列出所有分类（music 目录下的一级子文件夹名） */
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